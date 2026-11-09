package com.example.ailearning.l06decisions;

import com.example.ailearning.l06decisions.domain.ProductCatalog;

import com.example.ailearning.testsupport.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.*;
import static com.example.ailearning.testsupport.OpenAiChatStub.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=", "spring.ai.openai.max-retries=0"})
class WorkflowAgentLocalModelTest {
    static final OpenAiStubServer MODEL = new OpenAiStubServer();
    static final HttpClient HTTP = HttpClient.newHttpClient();
    private final TestOutput output = new TestOutput();
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
        HTTP.close();
        MODEL.close();
    }

    @Test
    void Workflow는_검색과_배송비_계산_후_예산을_검증한다() throws Exception {
        // Given: 조건 추출과 최종 설명에 사용할 응답
        MODEL
                .chat()
                .setResponseSequence(
                createAssistantTextMessage("{\"category\":\"음료\",\"totalBudget\":30000}"),
                createAssistantTextMessage("허브티를 추천합니다."));

        // When: 정해진 절차를 실행
        JsonNode result = post("WORKFLOW", "배송비 포함 3만원 이하 음료");

        // Then: 실제 서비스 호출 순서와 배송비 포함 예산 검증
        var callOrder = inOrder(productCatalog);
        callOrder.verify(productCatalog).search("음료", 30000);
        callOrder.verify(productCatalog).calculatePriceDetails("P01");
        callOrder.verify(productCatalog).calculatePriceDetails("P02");
        assertThat(result.path("mode").asString()).isEqualTo("WORKFLOW");
        assertThat(result.path("status").asString()).isEqualTo("COMPLETED");
        assertThat(result.path("offers").size()).isEqualTo(1);
        assertThat(result.at("/offers/0/product/id").asString()).isEqualTo("P01");
        assertThat(MODEL.chat().getRequests()).hasSize(2);
        for (JsonNode request : MODEL.chat().getRequests()) {
            assertThat(request.path("tools").isMissingNode() || request.path("tools").isEmpty()).isTrue();
        }
    }

    @Test
    void Agent는_도구_결과를_보고_다음_도구를_요청한다() throws Exception {
        scriptedAgent();
        JsonNode result = post("AGENT", "배송비 포함 3만원 이하 음료");
        assertThat(result.path("mode").asString()).isEqualTo("AGENT");
        assertThat(result.path("status").asString()).isEqualTo("COMPLETED");
        assertThat(MODEL.chat().getRequests()).hasSize(4);
        assertThat(result.path("offers").toString()).contains("31000", "15000");
        assertThat(MODEL.chat().getRequests().getLast().path("messages").toString()).contains("tool", "31000", "15000");
    }

    @Test
    void 혼합_방식은_모델이_잘못_추천해도_예산_초과_상품을_제외한다() throws Exception {
        scriptedAgent();
        JsonNode result = post("HYBRID", "배송비 포함 3만원 이하 음료");
        assertThat(result.path("mode").asString()).isEqualTo("HYBRID");
        assertThat(result.path("offers").size()).isEqualTo(1);
        assertThat(result.at("/offers/0/product/id").asString()).isEqualTo("P01");
        assertThat(result.path("answer").asString()).contains("P01").doesNotContain("P02");
    }

    @Test
    void 혼합_검색은_모델이_다른_조건을_요청해도_확정_조건을_사용한다() throws Exception {
        MODEL
                .chat()
                .setResponseSequence(
                        createAssistantTextMessage("{\"category\":\"음료\",\"totalBudget\":30000}"),
                        createAssistantToolCallMessage("search", "search", "{\"category\":\"생활\",\"maxPrice\":999999}"),
                        createAssistantToolCallMessage("shipping", "calculatePriceDetails", "{\"productId\":\"P01\"}"),
                        createAssistantTextMessage("허브티를 추천합니다."));
        JsonNode result = post("HYBRID", "3만원 이하 음료");
        verify(productCatalog).search("음료", 30000);
        assertThat(result.path("status").asString()).isEqualTo("COMPLETED");
        assertThat(result.at("/offers/0/product/id").asString()).isEqualTo("P01");
    }

    @Test
    void 예산이_없으면_검색하기_전에_추가_입력을_요청한다() throws Exception {
        MODEL
                .chat()
                .setResponseText("{\"category\":null,\"totalBudget\":null}");
        JsonNode result = post("WORKFLOW", "선물 추천");
        assertThat(result.path("status").asString()).isEqualTo("NEEDS_INPUT");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        verifyNoInteractions(productCatalog);
    }

    @Test
    void 검색_결과가_없으면_설명용_모델_호출을_생략한다() throws Exception {
        MODEL
                .chat()
                .setResponseText("{\"category\":\"음료\",\"totalBudget\":1000}");
        JsonNode result = post("WORKFLOW", "천원 이하 음료");
        assertThat(result.path("status").asString()).isEqualTo("NO_RESULTS");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        verify(productCatalog, never()).calculatePriceDetails(anyString());
    }

    @Test
    void 인사에는_도구가_필요_없고_조회한_금액은_요청별로_분리된다() throws Exception {
        scriptedAgent();
        assertThat(post("AGENT", "음료 추천").path("offers").size()).isEqualTo(2);
        clearInvocations(productCatalog);
        MODEL
                .chat()
                .setResponseText("안녕하세요.");
        JsonNode result = post("AGENT", "인사");
        assertThat(result.path("offers").isEmpty()).isTrue();
        verifyNoInteractions(productCatalog);
    }

    @Test
    void 혼합_방식에서_조회하지_않은_상품은_추천하지_않는다() throws Exception {
        MODEL
                .chat()
                .setResponseSequence(
                createAssistantTextMessage("{\"category\":\"음료\",\"totalBudget\":30000}"),
                createAssistantTextMessage("P01을 추천합니다."));
        assertThat(post("HYBRID", "음료 추천").path("status").asString()).isEqualTo("NO_VERIFIED_OFFER");
    }

    @Test
    void 모델_응답을_조건으로_변환하지_못하면_실패를_반환한다() throws Exception {
        MODEL
                .chat()
                .setResponseText("JSON이 아닙니다.");
        assertThat(post("WORKFLOW", "음료 추천").path("status").asString()).isEqualTo("FAILED");
        verifyNoInteractions(productCatalog);
    }
    @Test
    void 소문자_실행_방식은_모델_호출_전에_거부한다() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/compare/workflow"))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString("음료 추천"))
                .build();
        var response = output.printHttpResponse(HTTP.send(request, HttpResponse.BodyHandlers.ofString()));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(MODEL.chat().getRequests()).isEmpty();
        verifyNoInteractions(productCatalog);
    }

    void scriptedAgent() {
        MODEL
                .chat()
                .setResponseFunction(request -> {
                    String messages = request
                            .path("messages")
                            .toString();
                    if (messages.contains("TASK:EXTRACT")) return createAssistantTextMessage("{\"category\":\"음료\",\"totalBudget\":30000}");
                    int results = 0;
                    for (JsonNode message : request.path("messages")) if (message
                            .path("role")
                            .asString()
                            .equals("tool")) results++;
                    return switch (results) {
                        case 0 -> createAssistantToolCallMessage("search", "search", "{\"category\":\"음료\",\"maxPrice\":30000}");
                        case 1 -> createAssistantToolCallMessage("quote2", "calculatePriceDetails", "{\"productId\":\"P02\"}");
                        case 2 -> createAssistantToolCallMessage("quote1", "calculatePriceDetails", "{\"productId\":\"P01\"}");
                        default -> createAssistantTextMessage("P02를 추천합니다. 이 문장은 Java가 검증해야 합니다.");
                    };
                });
    }

    JsonNode post(String mode, String question) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/compare/" + mode))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(question))
                .build();
        var response = output.printHttpResponse(HTTP.send(request, HttpResponse.BodyHandlers.ofString()));
        output.printValue("모델 서버가 받은 요청 수", MODEL.chat().getRequests().size());
        assertThat(response.statusCode()).isEqualTo(200);
        return OpenAiStubServer.JSON.readTree(response.body());
    }
}
