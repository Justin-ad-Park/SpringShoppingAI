package com.example.ailearning.l06decisions;

import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.OpenAiStubServer;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantTextMessage;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantToolCallMessage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=",
        "learning.limits.max-model-calls=5", "learning.limits.max-tool-calls=3", "learning.limits.timeout-ms=1000"})
class WorkflowAgentLocalModelTest {
    private TestOutput output;

    @BeforeEach
    void 테스트_이름_준비(TestInfo info) {
        output = new TestOutput(info
                .getTestMethod()
                .orElseThrow()
                .getName());
    }

    static final OpenAiStubServer MODEL = new OpenAiStubServer();
    @Value("${local.server.port}") int port;
    @org.springframework.beans.factory.annotation.Autowired org.springframework.ai.chat.model.ChatModel model;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }
    @BeforeEach void reset() { MODEL
            .chat()
            .resetResponsesAndRequests(); }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void Workflow는_검색과_배송비_계산과_필터링_순서를_정한다() throws Exception {
        // Given: 추출과 답변 요청에 맞춘 가짜 모델 응답 설정
        MODEL
                .chat()
                .setResponseFunction(request -> createAssistantTextMessage(request
                                .path("messages")
                                .toString()
                                .contains("TASK:EXTRACT")
                ? "{\"category\":\"음료\",\"totalBudget\":30000}" : "허브티를 추천합니다."));

        // When: Workflow API 실행
        JsonNode result = post("workflow", "배송비 포함 3만원 이하 음료");

        // Then: Java가 정한 도구 순서와 결과·호출 횟수 검증
        assertThat(result.path("status").asString()).isEqualTo("COMPLETED");
        assertThat(result.path("offers").size()).isEqualTo(1);
        assertThat(result.at("/offers/0/product/id").asString()).isEqualTo("P01");
        assertThat(result.path("clientCalls").asInt()).isEqualTo(2);
        assertThat(result.path("modelCalls").asInt()).isEqualTo(2);
        assertThat(result.path("toolCalls").asInt()).isEqualTo(3);
        assertThat(result.path("steps").toString()).contains("searchProducts", "calculateShipping");
        for (JsonNode request : MODEL
                .chat()
                .getRequests()) assertThat(request.path("tools").isMissingNode() || request.path("tools").isEmpty()).isTrue();
    }
    @Test void Agent는_한_요청에서_도구를_선택하며_모델을_여러_번_호출한다() throws Exception {
        // Given: 가짜 모델의 검색·배송비 호출 응답 흐름 설정
        scriptedAgent();

        // When: Agent API 실행
        JsonNode result = post("agent", "배송비 포함 3만원 이하 음료");

        // Then: 도구 결과 재전달과 모델·도구 호출 횟수 검증
        assertThat(result.path("status").asString()).isEqualTo("COMPLETED");
        assertThat(result.path("clientCalls").asInt()).isEqualTo(1);
        assertThat(result.path("modelCalls").asInt()).isEqualTo(4);
        assertThat(result.path("toolCalls").asInt()).isEqualTo(3);
        assertThat(result.path("offers").toString()).contains("31000", "15000");
        assertThat(MODEL.chat().getRequests().getFirst().path("tools").toString()).contains("searchProducts", "calculateShipping");
    }
    @Test void 혼합_방식은_모델이_잘못_추천해도_예산_초과_상품을_제외한다() throws Exception {
        // Given: 예산 초과 상품을 추천하는 가짜 모델 응답 흐름 설정
        scriptedAgent();

        // When: 혼합 방식 API 실행
        JsonNode result = post("hybrid", "배송비 포함 3만원 이하 음료");

        // Then: Java 검증이 예산 초과 상품을 제외했는지 확인
        assertThat(result.path("status").asString()).isEqualTo("COMPLETED");
        assertThat(result.path("offers").size()).isEqualTo(1);
        assertThat(result.at("/offers/0/product/id").asString()).isEqualTo("P01");
        assertThat(result.path("answer").asString()).contains("P01").doesNotContain("P02");
    }
    @Test void 예산이_없거나_검색_결과가_없으면_Workflow를_중단한다() throws Exception {
        // Given: 예산이 없는 조건 추출 응답 설정
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("{\"category\":null,\"totalBudget\":null}"));

        // When: 필수 조건이 없는 Workflow 요청
        var missingBudget = post("workflow", "선물 추천");

        // Then: 추가 입력 요청으로 중단하고 모델을 한 번만 호출했는지 검증
        assertThat(missingBudget.path("status").asString()).isEqualTo("NEEDS_INPUT");
        assertThat(MODEL.chat().getRequests()).hasSize(1);

        // Given: 검색 결과가 없는 낮은 예산으로 모델 응답 변경
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("{\"category\":\"음료\",\"totalBudget\":1000}"));

        // When: 같은 Workflow에 낮은 예산으로 요청
        var empty = post("workflow", "천원 이하 음료");

        // Then: 검색 결과 없음으로 중단하고 추가 모델 호출이 없는지 검증
        assertThat(empty.path("status").asString()).isEqualTo("NO_RESULTS");
        assertThat(empty.path("modelCalls").asInt()).isEqualTo(1);
    }
    @Test void 도구_없이_답할_수_있고_호출_기록은_요청별로_분리된다() throws Exception {
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("안녕하세요."));
        assertThat(post("agent", "인사").path("toolCalls").asInt()).isZero();
        assertThat(post("agent", "다시 인사").path("modelCalls").asInt()).isEqualTo(1);
    }
    @Test void 도구를_반복_요청해도_Java의_실행_상한을_넘지_못한다() throws Exception {
        // Given: 같은 도구를 계속 요청하는 가짜 모델 설정
        MODEL
                .chat()
                .setResponseFunction(request -> createAssistantToolCallMessage("repeat", "searchProducts", "{\"category\":\"음료\"}"));

        // When: Agent API 실행
        var result = post("agent", "계속 검색");

        // Then: 도구 실행 상한에서 중단했는지 검증
        assertThat(result.path("status").asString()).isEqualTo("TOOL_LIMIT");
        assertThat(result.path("toolCalls").asInt()).isEqualTo(3);
        assertThat(result.path("modelCalls").asInt()).isLessThanOrEqualTo(5);
    }
    @Test void 모델_호출_상한에_도달하면_다음_호출을_중단한다() {
        // Given: 조건 추출 응답과 모델 호출 상한이 1인 서비스 준비
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("{\"category\":\"음료\",\"totalBudget\":30000}"));
        var service = new ComparisonService(model, new ProductCatalog(), 1, 3, 1000);

        // When: 서비스의 Workflow 실행
        var result = service.run("workflow", "배송비 포함 3만원 이하 음료");
        output.printValue("호출 상한 결과", result);

        // Then: 모델을 한 번만 호출하고 상한에서 중단했는지 검증
        assertThat(result.status()).isEqualTo("MODEL_LIMIT");
        assertThat(result.modelCalls()).isEqualTo(1);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }
    @Test void 대기_시간을_초과한_뒤에는_도구를_실행하지_않는다() throws Exception {
        // Given: 제한 시간보다 늦게 도구 호출을 요청하는 모델 설정
        MODEL
                .chat()
                .setResponseFunction(request -> {
                    try { Thread.sleep(1600); } catch (InterruptedException error) { Thread.currentThread()
                            .interrupt(); }
                    return createAssistantToolCallMessage("late", "searchProducts", "{}");
                });

        // When: 시간을 측정하며 Agent API 실행
        long started = System.nanoTime();
        var result = post("agent", "느린 응답");

        // Then: 시간 초과 응답과 늦은 응답 이후에도 도구가 실행되지 않음을 검증
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        output.printValue("경과 시간(ms)", elapsedMs);
        assertThat(result.path("status").asString()).isEqualTo("TIMEOUT");
        assertThat(elapsedMs).isLessThan(1500);
        assertThat(result.path("toolCalls").asInt()).isZero();
        Thread.sleep(800); // 늦은 응답 이후에도 새 모델 요청/도구 실행을 시작하지 않음.
        assertThat(MODEL.chat().getRequests()).hasSize(1);
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
                        case 0 -> createAssistantToolCallMessage("search", "searchProducts", "{\"category\":\"음료\",\"maxPrice\":30000}");
                        case 1 -> createAssistantToolCallMessage("quote2", "calculateShipping", "{\"productId\":\"P02\"}");
                        case 2 -> createAssistantToolCallMessage("quote1", "calculateShipping", "{\"productId\":\"P01\"}");
                        default -> createAssistantTextMessage("P02를 추천합니다. 이 문장은 Java가 검증해야 합니다.");
                    };
                });
    }
    JsonNode post(String mode, String question) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            var response = output.printHttpResponse(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/compare/" + mode))
                                    .header("Content-Type", "text/plain; charset=UTF-8")
                                    .POST(HttpRequest.BodyPublishers.ofString(question))
                                    .build(), HttpResponse.BodyHandlers.ofString()));
            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
            return OpenAiStubServer.JSON.readTree(response.body());
        }
    }
}
