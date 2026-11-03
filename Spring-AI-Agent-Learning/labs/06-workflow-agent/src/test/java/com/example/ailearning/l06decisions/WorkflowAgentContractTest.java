package com.example.ailearning.l06decisions;

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
class WorkflowAgentContractTest {
    static final LocalModelServer MODEL = start();
    @Value("${local.server.port}") int port;
    @org.springframework.beans.factory.annotation.Autowired org.springframework.ai.chat.model.ChatModel model;
    static LocalModelServer start() {
        try { return new LocalModelServer(); } catch (Exception error) { throw new IllegalStateException(error); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::url);
    }
    @BeforeEach void reset() { MODEL.requests.clear(); }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void workflowForcesSearchQuoteFilterSummaryAndDoesNotExposeToolsToModel() throws Exception {
        MODEL.responder = request -> LocalModelServer.message(request.path("messages").toString().contains("TASK:EXTRACT")
                ? "{\"category\":\"음료\",\"totalBudget\":30000}" : "허브티를 추천합니다.");
        JsonNode result = post("workflow", "배송비 포함 3만원 이하 음료");
        assertThat(result.path("status").asString()).isEqualTo("COMPLETED");
        assertThat(result.path("offers").size()).isEqualTo(1);
        assertThat(result.at("/offers/0/product/id").asString()).isEqualTo("P01");
        assertThat(result.path("clientCalls").asInt()).isEqualTo(2);
        assertThat(result.path("modelCalls").asInt()).isEqualTo(2);
        assertThat(result.path("toolCalls").asInt()).isEqualTo(3);
        assertThat(result.path("steps").toString()).contains("searchProducts", "calculateShipping");
        for (JsonNode request : MODEL.requests) assertThat(request.path("tools").isMissingNode() || request.path("tools").isEmpty()).isTrue();
    }
    @Test void agentChoosesToolsAndUsesSeveralModelCallsInsideOneClientCall() throws Exception {
        scriptedAgent();
        JsonNode result = post("agent", "배송비 포함 3만원 이하 음료");
        assertThat(result.path("status").asString()).isEqualTo("COMPLETED");
        assertThat(result.path("clientCalls").asInt()).isEqualTo(1);
        assertThat(result.path("modelCalls").asInt()).isEqualTo(4);
        assertThat(result.path("toolCalls").asInt()).isEqualTo(3);
        assertThat(result.path("offers").toString()).contains("31000", "15000");
        assertThat(MODEL.requests.getFirst().path("tools").toString()).contains("searchProducts", "calculateShipping");
    }
    @Test void hybridFiltersOverBudgetFactsEvenWhenModelNamesWrongProduct() throws Exception {
        scriptedAgent();
        JsonNode result = post("hybrid", "배송비 포함 3만원 이하 음료");
        assertThat(result.path("status").asString()).isEqualTo("COMPLETED");
        assertThat(result.path("offers").size()).isEqualTo(1);
        assertThat(result.at("/offers/0/product/id").asString()).isEqualTo("P01");
        assertThat(result.path("answer").asString()).contains("P01").doesNotContain("P02");
    }
    @Test void missingBudgetAndNoMatchesStopWorkflowBeforeUnnecessaryCalls() throws Exception {
        MODEL.responder = request -> LocalModelServer.message("{\"category\":null,\"totalBudget\":null}");
        assertThat(post("workflow", "선물 추천").path("status").asString()).isEqualTo("NEEDS_INPUT");
        assertThat(MODEL.requests).hasSize(1);
        MODEL.responder = request -> LocalModelServer.message("{\"category\":\"음료\",\"totalBudget\":1000}");
        var empty = post("workflow", "천원 이하 음료");
        assertThat(empty.path("status").asString()).isEqualTo("NO_RESULTS");
        assertThat(empty.path("modelCalls").asInt()).isEqualTo(1);
    }
    @Test void agentCanChooseNoToolAndCallsAreIsolatedPerRequest() throws Exception {
        MODEL.responder = request -> LocalModelServer.message("안녕하세요.");
        assertThat(post("agent", "인사").path("toolCalls").asInt()).isZero();
        assertThat(post("agent", "다시 인사").path("modelCalls").asInt()).isEqualTo(1);
    }
    @Test void repeatedToolRequestsCannotBypassJavaToolLimit() throws Exception {
        MODEL.responder = request -> LocalModelServer.tool("repeat", "searchProducts", "{\"category\":\"음료\"}");
        var result = post("agent", "계속 검색");
        assertThat(result.path("status").asString()).isEqualTo("TOOL_LIMIT");
        assertThat(result.path("toolCalls").asInt()).isEqualTo(3);
        assertThat(result.path("modelCalls").asInt()).isLessThanOrEqualTo(5);
    }
    @Test void modelLimitStopsWorkflowBeforeSecondModelCall() {
        MODEL.responder = request -> LocalModelServer.message("{\"category\":\"음료\",\"totalBudget\":30000}");
        var service = new ComparisonService(model, new ProductCatalog(), 1, 3, 1000);
        var result = service.run("workflow", "배송비 포함 3만원 이하 음료");
        assertThat(result.status()).isEqualTo("MODEL_LIMIT");
        assertThat(result.modelCalls()).isEqualTo(1);
        assertThat(MODEL.requests).hasSize(1);
    }
    @Test void totalWaitIsBoundedAndLateModelResponseCannotExecuteTool() throws Exception {
        MODEL.responder = request -> {
            try { Thread.sleep(1600); } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
            return LocalModelServer.tool("late", "searchProducts", "{}");
        };
        long started = System.nanoTime();
        var result = post("agent", "느린 응답");
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        assertThat(result.path("status").asString()).isEqualTo("TIMEOUT");
        assertThat(elapsedMs).isLessThan(1500);
        assertThat(result.path("toolCalls").asInt()).isZero();
        Thread.sleep(800); // 늦은 응답 이후에도 새 모델 요청/도구 실행을 시작하지 않음.
        assertThat(MODEL.requests).hasSize(1);
    }
    void scriptedAgent() {
        MODEL.responder = request -> {
            String messages = request.path("messages").toString();
            if (messages.contains("TASK:EXTRACT")) return LocalModelServer.message("{\"category\":\"음료\",\"totalBudget\":30000}");
            int results = 0;
            for (JsonNode message : request.path("messages")) if (message.path("role").asString().equals("tool")) results++;
            return switch (results) {
                case 0 -> LocalModelServer.tool("search", "searchProducts", "{\"category\":\"음료\",\"maxPrice\":30000}");
                case 1 -> LocalModelServer.tool("quote2", "calculateShipping", "{\"productId\":\"P02\"}");
                case 2 -> LocalModelServer.tool("quote1", "calculateShipping", "{\"productId\":\"P01\"}");
                default -> LocalModelServer.message("P02를 추천합니다. 이 문장은 Java가 검증해야 합니다.");
            };
        };
    }
    JsonNode post(String mode, String question) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            var response = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/compare/" + mode))
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(question)).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
            return LocalModelServer.JSON.readTree(response.body());
        }
    }
}
