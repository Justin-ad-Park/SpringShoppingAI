package com.example.ailearning.l13shopping.lesson11_agent;

import static com.example.ailearning.testsupport.model.OpenAiChatStub.createAssistantTextMessage;
import static com.example.ailearning.testsupport.model.OpenAiChatStub.createAssistantToolCallMessage;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.ailearning.testsupport.model.OpenAiStubServer;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.config.import=")
class Lesson11ShoppingAgentLocalModelTest {
    private static final OpenAiStubServer MODEL = startModelServer();
    private static final String SHOPPING_AND_RETURN_ANSWER = """
            허브티를 추천합니다. 배송비 포함 총액은 15000원입니다.
            미개봉 상품은 수령 후 7일 이내 반품할 수 있습니다. 출처는 policies.md#return-sealed입니다.
            """;

    @Autowired
    Lesson11ShoppingAgent shoppingAgent;

    @DynamicPropertySource
    static void 모델_접속_설정(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.embedding.options.model", () -> "learning-embedding");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL
                .chat()
                .resetResponsesAndRequests();
    }

    @AfterAll
    static void 서버_종료() {
        MODEL.close();
    }

    @Test
    void 한_질문에서_상품_총액과_미개봉_반품_기간을_조회해_함께_답한다() {
        prepareShoppingAndReturnResponses();

        String answer = shoppingAgent.answer("3만원 이하 음료를 추천하고 미개봉 반품 기간도 알려줘.", List.of());

        System.out.println(answer);
        assertThat(MODEL.chat().getRequests()).hasSize(4);
        assertShoppingAndReturnResults(answer);
    }

    @Test
    void 예산과_개봉_여부를_묻고_고객이_답하면_두_조회를_이어서_처리한다() {
        String firstQuestion = "음료 추천해 주고 반품 기간도 알려줘.";
        int embeddingRequestsBefore = MODEL.embeddings().getRequests().size();
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("배송비 포함 예산은 얼마인가요? 반품할 상품은 개봉했나요?"));

        String clarification = shoppingAgent.answer(firstQuestion, List.of());

        System.out.println("확인 질문: " + clarification);
        assertThat(clarification).contains("예산", "개봉");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        assertThat(MODEL.embeddings().getRequests()).hasSize(embeddingRequestsBefore);

        // 호출자가 앞선 고객 질문과 모델이 반환한 확인 질문을 보관하고 다음 요청에 넣는다.
        List<Message> previousMessages = List.of(new UserMessage(firstQuestion), new AssistantMessage(clarification));
        String customerReply = "배송비 포함 3만원이고, 개봉하지 않았어요.";
        prepareShoppingAndReturnResponses();

        String answer = shoppingAgent.answer(customerReply, previousMessages);

        System.out.println("이어서 받은 답변:\n" + answer);
        assertThat(MODEL.chat().getRequests()).hasSize(5);
        var followUpRequest = MODEL.chat().getRequests().get(1);
        var conversationMessages = followUpRequest
                .path("messages")
                .valueStream()
                .filter(message -> !message.path("role").asString().equals("system"))
                .toList();
        assertThat(conversationMessages)
                .extracting(message -> message.path("role").asString())
                .containsExactly("user", "assistant", "user");
        assertThat(conversationMessages)
                .extracting(message -> message.path("content").asString())
                .containsExactly(firstQuestion, clarification, customerReply);
        assertShoppingAndReturnResults(answer);
    }

    @Test
    void 반품_기간만_물으면_상품_도구_없이_정책만_조회한다() {
        MODEL
                .chat()
                .setResponseSequence(
                        createAssistantToolCallMessage("return-1", "searchReturnPolicy", """
                                {"question":"미개봉 상품 반품 기간"}
                                """),
                        createAssistantTextMessage("미개봉 상품은 수령 후 7일 이내 반품할 수 있습니다. 출처는 policies.md#return-sealed입니다."));

        String answer = shoppingAgent.answer("미개봉 반품 기간만 알려줘.", List.of());

        System.out.println(answer);
        assertThat(MODEL.chat().getRequests()).hasSize(2);
        var toolResults = toolResultsSentBeforeFinalAnswer();
        assertThat(toolResults).hasSize(1);
        assertThat(toolResults.getFirst().path("tool_call_id").asString()).isEqualTo("return-1");
        assertThat(toolResults.getFirst().path("content").asString()).contains("return-sealed", "수령 후 7일");
        assertThat(answer).contains("수령 후 7일 이내", "policies.md#return-sealed");
    }

    private void prepareShoppingAndReturnResponses() {
        MODEL
                .chat()
                .setResponseSequence(
                        createAssistantToolCallMessage("products-1", "searchProducts", """
                                {"category":"음료","maxProductPrice":30000}
                                """),
                        createAssistantToolCallMessage("shipping-1", "calculateShipping", """
                                {"productId":"P01"}
                                """),
                        createAssistantToolCallMessage("return-1", "searchReturnPolicy", """
                                {"question":"미개봉 상품 반품 기간"}
                                """),
                        createAssistantTextMessage(SHOPPING_AND_RETURN_ANSWER));
    }

    private void assertShoppingAndReturnResults(String answer) {
        List<JsonNode> toolResults = toolResultsSentBeforeFinalAnswer();
        assertThat(toolResults)
                .extracting(message -> message.path("tool_call_id").asString())
                .containsExactly("products-1", "shipping-1", "return-1");
        assertThat(toolResults.get(0).path("content").asString()).contains("P01", "허브티");
        assertThat(toolResults.get(1).path("content").asString()).contains("P01", "\"total\":15000");
        assertThat(toolResults.get(2).path("content").asString()).contains("return-sealed", "수령 후 7일");
        assertThat(answer).contains("허브티", "배송비 포함 총액은 15000원", "수령 후 7일 이내", "policies.md#return-sealed");
    }

    private List<JsonNode> toolResultsSentBeforeFinalAnswer() {
        return MODEL
                .chat()
                .getRequests()
                .getLast()
                .path("messages")
                .valueStream()
                .filter(message -> message.path("role").asString().equals("tool"))
                .toList();
    }

    private static OpenAiStubServer startModelServer() {
        var server = new OpenAiStubServer();
        server
                .embeddings()
                .setVectorFunction(text -> {
                    if (text.contains("미개봉")) {
                        return new float[]{1, 0, 0};
                    }
                    if (text.contains("개봉한")) {
                        return new float[]{0, 1, 0};
                    }
                    return new float[]{0, 0, 1};
                });
        return server;
    }
}
