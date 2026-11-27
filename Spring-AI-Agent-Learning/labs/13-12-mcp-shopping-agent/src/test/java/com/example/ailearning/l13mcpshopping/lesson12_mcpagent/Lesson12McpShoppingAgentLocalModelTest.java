package com.example.ailearning.l13mcpshopping.lesson12_mcpagent;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.config.import=", "spring.ai.mcp.server.enabled=false"})
class Lesson12McpShoppingAgentLocalModelTest {
    private static final ShoppingMcpTestServer MCP_SERVER = new ShoppingMcpTestServer();
    private static final OpenAiStubServer MODEL = new OpenAiStubServer();
    private static final String RETURN_POLICY_ANSWER = "미개봉 상품은 수령 후 7일 이내 반품할 수 있습니다. 출처는 return-sealed입니다.";
    private static final String SHOPPING_ANSWER = "허브티 컬렉션을 추천합니다. 상품가 9900원, 배송비 3000원, 배송비 포함 총액은 12900원입니다.";

    @Autowired
    Lesson12McpShoppingAgent shoppingAgent;

    @DynamicPropertySource
    static void 접속_설정(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
        registry.add("spring.ai.mcp.client.streamable-http.connections.shopping.url", MCP_SERVER::getBaseUrl);
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
        MCP_SERVER.close();
    }

    @Test
    void 상품_총액과_미개봉_반품_기간을_MCP로_조회해_함께_답한다() {
        prepareShoppingAndReturnResponses();

        String answer = shoppingAgent.answer("3만원 이하 음료를 추천하고 미개봉 반품 기간도 알려줘.", List.of());

        System.out.println(answer);
        var modelRequests = MODEL.chat().getRequests();
        assertThat(modelRequests).hasSize(4);
        var offeredTools = modelRequests
                .getFirst()
                .path("tools")
                .valueStream()
                .map(tool -> tool.path("function").path("name").asString())
                .toList();
        assertThat(offeredTools).containsExactlyInAnyOrder("searchProducts", "quoteCart", "getReturnPolicy");
        assertShoppingAndReturnResults(answer);
    }

    @Test
    void 확인_질문에_고객이_답하면_이전_대화를_참고해_MCP_조회를_시작한다() {
        String firstQuestion = "음료 추천해 주고 반품 기간도 알려줘.";
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("배송비 포함 예산은 얼마인가요? 반품할 상품은 개봉했나요?"));

        String clarification = shoppingAgent.answer(firstQuestion, List.of());

        System.out.println("확인 질문: " + clarification);
        assertThat(clarification).contains("예산", "개봉");
        assertThat(MODEL.chat().getRequests()).hasSize(1);

        List<Message> previousMessages = List.of(new UserMessage(firstQuestion), new AssistantMessage(clarification));
        String customerReply = "배송비 포함 3만원이고, 개봉하지 않았어요.";
        prepareShoppingAndReturnResponses();

        String answer = shoppingAgent.answer(customerReply, previousMessages);

        System.out.println("이어서 받은 답변:\n" + answer);
        var modelRequests = MODEL.chat().getRequests();
        assertThat(modelRequests).hasSize(5);
        var conversationMessages = modelRequests
                .get(1)
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
    void 미개봉_반품_기간만_물으면_MCP의_반품_도구만_실행한다() {
        MODEL
                .chat()
                .setResponseSequence(
                        createAssistantToolCallMessage("return-1", "getReturnPolicy", """
                                {"packagingCondition":"SEALED"}
                                """),
                        createAssistantTextMessage(RETURN_POLICY_ANSWER));

        String answer = shoppingAgent.answer("미개봉 반품 기간만 알려줘.", List.of());

        System.out.println(answer);
        assertThat(MODEL.chat().getRequests()).hasSize(2);
        var toolResults = toolResultsSentBeforeFinalAnswer();
        assertThat(toolResults).hasSize(1);
        assertThat(toolResults.getFirst().path("tool_call_id").asString()).isEqualTo("return-1");
        assertThat(toolResults.getFirst().path("content").asString()).contains("return-sealed", "수령 후 7일 이내");
        assertThat(answer).contains("수령 후 7일 이내", "return-sealed");
    }

    private void prepareShoppingAndReturnResponses() {
        MODEL
                .chat()
                .setResponseSequence(
                        createAssistantToolCallMessage("products-1", "searchProducts", """
                                {"query":{"category":"음료","maxPrice":30000,"shippingGroups":["MALL"]}}
                                """),
                        createAssistantToolCallMessage("quote-1", "quoteCart", """
                                {"scenario":"empty","additionalProductIds":["P004"]}
                                """),
                        createAssistantToolCallMessage("return-1", "getReturnPolicy", """
                                {"packagingCondition":"SEALED"}
                                """),
                        createAssistantTextMessage(SHOPPING_ANSWER + "\n" + RETURN_POLICY_ANSWER));
    }

    private void assertShoppingAndReturnResults(String answer) {
        var toolResults = toolResultsSentBeforeFinalAnswer();
        assertThat(toolResults)
                .extracting(message -> message.path("tool_call_id").asString())
                .containsExactly("products-1", "quote-1", "return-1");
        assertThat(toolResults.get(0).path("content").asString()).contains("P004", "허브티 컬렉션");
        assertThat(toolResults.get(1).path("content").asString()).contains("9900", "3000", "12900");
        assertThat(toolResults.get(2).path("content").asString()).contains("return-sealed", "수령 후 7일 이내");
        assertThat(answer).contains("허브티 컬렉션", "배송비 3000원", "배송비 포함 총액은 12900원", "수령 후 7일 이내", "return-sealed");
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
}
