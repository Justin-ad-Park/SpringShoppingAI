package com.example.ailearning.l06decisions;

import com.example.ailearning.l06decisions.domain.ProductCatalog;

import com.example.ailearning.testsupport.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;
import static com.example.ailearning.testsupport.OpenAiChatStub.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=", "spring.ai.openai.max-retries=0"})
class WorkflowAgentLocalModelTest {
    static final OpenAiStubServer MODEL = new OpenAiStubServer();
    private final TestOutput output = new TestOutput();

    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> this.port, output);

    @Value("${local.server.port}") int port;
    @MockitoSpyBean ProductCatalog productCatalog;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
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
    static void 종료() {
        MODEL.close();
    }

    private static final String SHOPPING_QUESTION = "친구에게 줄 음료 선물을 찾고 있어요. 배송비까지 3만 원 안에서 추천해 주세요.";
    private static final String CONDITION = "{\"category\":\"음료\",\"totalBudget\":30000}";

    @Test
    void Workflow는_고정_순서로_조회하고_예산_안의_후보만_모델에_전달한다() throws Exception {
        MODEL.chat().setResponseSequence(
                createAssistantTextMessage(CONDITION),
                createAssistantTextMessage("허브티를 추천합니다."));

        JsonNode result = post("WORKFLOW", SHOPPING_QUESTION);

        var order = inOrder(productCatalog);
        order.verify(productCatalog).search("음료", 30000);
        order.verify(productCatalog).calculatePriceDetail("P01");
        order.verify(productCatalog).calculatePriceDetail("P02");
        verifyNoMoreInteractions(productCatalog);
        assertThat(result.path("status").asString()).isEqualTo("COMPLETED");
        assertThat(MODEL.chat().getRequests()).hasSize(2);
        assertThat(MODEL.chat().getRequests().getLast().path("messages").toString())
                .contains("P01", "15000").doesNotContain("P02", "31000");
        for (JsonNode request : MODEL.chat().getRequests()) {
            assertThat(request.path("tools").isMissingNode() || request.path("tools").isEmpty()).isTrue();
        }
    }

    @Test
    void Agent는_검색과_배송비_도구_결과를_받아_답변한다() throws Exception {
        MODEL.chat().setResponseSequence(
                createAssistantToolCallMessage("search", "searchProducts", "{\"category\":\"음료\",\"maxPrice\":30000}"),
                createAssistantToolCallMessage("shipping2", "calculateShipping", "{\"productId\":\"P02\"}"),
                createAssistantToolCallMessage("shipping1", "calculateShipping", "{\"productId\":\"P01\"}"),
                createAssistantTextMessage("배송비까지 15000원인 허브티를 추천합니다."));

        JsonNode result = post("AGENT", SHOPPING_QUESTION);

        assertThat(result.path("answer").asString()).contains("허브티");
        assertThat(result.path("status").asString()).isEqualTo("COMPLETED");
        assertThat(MODEL.chat().getRequests()).hasSize(4);
        assertThat(MODEL.chat().getRequests().getFirst().path("tools").size()).isEqualTo(2);
        assertThat(MODEL.chat().getRequests().getLast().path("messages").toString()).contains("tool", "31000", "15000");
        verify(productCatalog).search("음료", 30000);
        verify(productCatalog).calculatePriceDetail("P01");
        verify(productCatalog).calculatePriceDetail("P02");
    }

    @Test
    void Hybrid는_조건을_고정하고_예산을_통과한_도구_결과만_모델에_전달한다() throws Exception {
        MODEL.chat().setResponseSequence(
                createAssistantTextMessage(CONDITION),
                createAssistantToolCallMessage("candidates", "findProductsWithinBudget", "{}"),
                createAssistantTextMessage("허브티는 배송비까지 15000원입니다."));

        JsonNode result = post("HYBRID", SHOPPING_QUESTION);

        assertThat(result.path("status").asString()).isEqualTo("COMPLETED");
        assertThat(result.path("answer").asString()).contains("허브티");
        assertThat(MODEL.chat().getRequests()).hasSize(3);
        JsonNode tool = MODEL.chat().getRequests().get(1).path("tools").get(0);
        assertThat(tool.at("/function/name").asString()).isEqualTo("findProductsWithinBudget");
        // 모델이 검색 조건이나 금액을 인자로 다시 지정할 수 없는 도구다.
        assertThat(tool.at("/function/parameters/properties").isEmpty()).isTrue();
        assertThat(MODEL.chat().getRequests().getLast().path("messages").toString())
                .contains("P01", "15000").doesNotContain("P02", "31000", "P03");
        verify(productCatalog).search("음료", 30000);
        verify(productCatalog).calculatePriceDetail("P01");
        verify(productCatalog).calculatePriceDetail("P02");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"WORKFLOW", "HYBRID"})
    void 예산이_없으면_검색하기_전에_추가_입력을_요청한다(String mode) throws Exception {
        MODEL.chat().setResponseText("{\"category\":\"음료\",\"totalBudget\":null}");
        JsonNode result = post(mode, "친구에게 줄 음료 선물을 추천해 주세요.");
        assertThat(result.path("status").asString()).isEqualTo("NEEDS_INPUT");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        verifyNoInteractions(productCatalog);
    }

    @Test
    void Workflow는_검색_결과가_없으면_설명용_모델_호출을_생략한다() throws Exception {
        MODEL.chat().setResponseText("{\"category\":\"음료\",\"totalBudget\":1000}");
        JsonNode result = post("WORKFLOW", "배송비까지 천 원 안에서 음료 선물을 추천해 주세요.");
        assertThat(result.path("status").asString()).isEqualTo("NO_RESULTS");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        verify(productCatalog).search("음료", 1000);
        verify(productCatalog, never()).calculatePriceDetail(anyString());
    }

    @Test
    void Hybrid는_빈_조회_결과를_모델에_전달해_설명한다() throws Exception {
        MODEL.chat().setResponseSequence(
                createAssistantTextMessage("{\"category\":\"음료\",\"totalBudget\":1000}"),
                createAssistantToolCallMessage("candidates", "findProductsWithinBudget", "{}"),
                createAssistantTextMessage("해당 예산에 맞는 음료가 없습니다."));
        JsonNode result = post("HYBRID", "배송비까지 천 원 안에서 음료 선물을 추천해 주세요.");
        assertThat(result.path("status").asString()).isEqualTo("COMPLETED");
        JsonNode last = MODEL.chat().getRequests().getLast();
        JsonNode messages = last.path("messages");
        JsonNode toolResult = messages.get(messages.size() - 1);
        assertThat(toolResult.path("role").asString()).isEqualTo("tool");
        assertThat(toolResult.path("content").asString()).isEqualTo("[]");
        verify(productCatalog).search("음료", 1000);
        verify(productCatalog, never()).calculatePriceDetail(anyString());
    }

    @Test
    void Agent는_상품_이름만_물으면_배송비_계산_없이_검색한다() throws Exception {
        MODEL.chat().setResponseSequence(
                createAssistantToolCallMessage("search", "searchProducts", "{\"category\":\"음료\",\"maxPrice\":null}"),
                createAssistantTextMessage("허브티와 커피가 있습니다."));
        JsonNode result = post("AGENT", "어떤 음료 상품들이 있나요? 이름만 알려 주세요.");
        assertThat(result.path("answer").asString()).contains("허브티", "커피");
        verify(productCatalog).search("음료", null);
        verify(productCatalog, never()).calculatePriceDetail(anyString());
    }

    @Test
    void Agent는_인사에_도구_없이_답변한다() throws Exception {
        MODEL.chat().setResponseText("안녕하세요. 선물 찾기를 도와드릴게요.");
        assertThat(post("AGENT", "안녕하세요! 어떤 도움을 받을 수 있나요?").path("answer").asString()).isNotBlank();
        verifyNoInteractions(productCatalog);
    }

    @Test
    void 모델_응답을_조건으로_변환하지_못하면_실패를_반환한다() throws Exception {
        MODEL.chat().setResponseText("JSON이 아닙니다.");
        assertThat(post("WORKFLOW", SHOPPING_QUESTION).path("status").asString()).isEqualTo("FAILED");
        verifyNoInteractions(productCatalog);
    }

    @Test
    void 소문자_실행_방식은_모델_호출_전에_거부한다() throws Exception {
        var response = appClient.postText("/api/compare/workflow", "음료 추천");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(MODEL.chat().getRequests()).isEmpty();
        verifyNoInteractions(productCatalog);
    }

    JsonNode post(String mode, String question) throws Exception {
        var response = appClient.postText("/api/compare/" + mode, question);
        output.printValue("모델 서버가 받은 요청 수", MODEL.chat().getRequests().size());
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode result = OpenAiStubServer.JSON.readTree(response.body());
        assertThat(result.path("mode").asString()).isEqualTo(mode);
        return result;
    }
}
