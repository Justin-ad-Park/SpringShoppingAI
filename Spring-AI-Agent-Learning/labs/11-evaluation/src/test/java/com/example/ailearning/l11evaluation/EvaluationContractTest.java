package com.example.ailearning.l11evaluation;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.URI;
import java.net.http.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.config.import=", "spring.ai.openai.chat.timeout=500ms"})
class EvaluationContractTest {
    static final ModelWireServer MODEL = start();
    @Value("${local.server.port}") int port;
    @org.springframework.beans.factory.annotation.Autowired RagService service;

    static ModelWireServer start() {
        try { return new ModelWireServer(); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.embedding.options.model", () -> "learning-embedding");
        registry.add("spring.ai.openai.base-url", MODEL::url);
    }
    @BeforeEach void reset() { MODEL.requests.clear(); MODEL.chatStatus = 200; MODEL.chatDelayMs = 0; }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void fixedCasesPassAndExposeActualUsageAndLatency() throws Exception {
        MODEL.responder = request -> ModelWireServer.message(request.path("messages").toString().contains("shipping]")
                ? "{\"answerable\":true,\"answer\":\"3000원\",\"sourceIds\":[\"shipping\"]}"
                : "{\"answerable\":true,\"answer\":\"7일 이내\",\"sourceIds\":[\"return-sealed\"]}");
        var report = run();
        assertThat(report.path("total").asInt()).isEqualTo(3);
        assertThat(report.path("passed").asInt()).isEqualTo(3);
        assertThat(report.at("/cases/0/actual/metrics/inputTokens").asInt()).isEqualTo(10);
        assertThat(report.at("/cases/0/actual/metrics/elapsedMs").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(report.at("/cases/2/actual/metrics/chatCalls").asInt()).isZero();
        assertThat(MODEL.requests).hasSize(2);
    }
    @Test void validCitationButWrongClaimFailsRuleEvaluation() throws Exception {
        MODEL.responder = request -> ModelWireServer.message("{\"answerable\":true,\"answer\":\"반품은 30일입니다.\",\"sourceIds\":[\"return-sealed\"]}");
        var report = run();
        assertThat(report.path("passed").asInt()).isLessThan(3);
        assertThat(report.at("/cases/1/failures").toString()).contains("expected text missing");
    }
    @Test void rateLimitIsFailureNotSuccessfulFallbackAndSdkDoesNotRetry() throws Exception {
        MODEL.chatStatus = 429;
        var report = run();
        assertThat(report.at("/cases/0/actual/reply/status").asString()).isEqualTo("FAILED");
        assertThat(report.at("/cases/0/actual/metrics/failureKind").asString()).isEqualTo("RATE_LIMIT");
        assertThat(MODEL.requests).hasSize(2);
    }
    @Test void malformedOutputIsRecordedAsFailureWithoutLeakingExceptionMessage() throws Exception {
        MODEL.responder = request -> ModelWireServer.message("not json");
        var response = post("반품");
        assertThat(response.statusCode()).isEqualTo(200);
        var body = ModelWireServer.JSON.readTree(response.body());
        assertThat(body.at("/reply/status").asString()).isEqualTo("FAILED");
        assertThat(body.at("/metrics/failureKind").asString()).isEqualTo("UPSTREAM_OR_OUTPUT");
        assertThat(body.at("/metrics/inputTokens").isMissingNode() || body.at("/metrics/inputTokens").isNull()).isTrue();
    }
    @Test void slowModelIsRecordedAsTimeoutWithoutAutomaticRetry() throws Exception {
        MODEL.chatDelayMs = 1100;
        var response = post("반품");
        var body = ModelWireServer.JSON.readTree(response.body());
        assertThat(body.at("/reply/status").asString()).isEqualTo("FAILED");
        assertThat(body.at("/metrics/failureKind").asString()).isEqualTo("TIMEOUT");
        assertThat(MODEL.requests).hasSize(1);
        Thread.sleep(800);
    }
    tools.jackson.databind.JsonNode run() throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            var response = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/evaluation/run"))
                    .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            return ModelWireServer.JSON.readTree(response.body());
        }
    }
    HttpResponse<String> post(String question) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/evaluation/ask"))
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(question)).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
