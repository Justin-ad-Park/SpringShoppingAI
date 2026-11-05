package com.example.ailearning.l11evaluation;

import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.OpenAiStubServer;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantTextMessage;
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
    private TestOutput output;

    @BeforeEach
    void 테스트_이름_준비(TestInfo info) {
        output = new TestOutput(info
                .getTestMethod()
                .orElseThrow()
                .getName());
    }

    static final OpenAiStubServer MODEL = createModelServer();
    @Value("${local.server.port}") int port;
    @org.springframework.beans.factory.annotation.Autowired RagService service;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.embedding.options.model", () -> "learning-embedding");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }
    @BeforeEach void reset() { MODEL
            .chat()
            .resetResponsesAndRequests(); }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void 고정_평가_사례를_비교하고_토큰과_지연을_기록한다() throws Exception {
        MODEL
                .chat()
                .setResponseFunction(request -> createAssistantTextMessage(request
                                .path("messages")
                                .toString()
                                .contains("shipping]")
                ? "{\"answerable\":true,\"answer\":\"3000원\",\"sourceIds\":[\"shipping\"]}"
                : "{\"answerable\":true,\"answer\":\"7일 이내\",\"sourceIds\":[\"return-sealed\"]}"));
        var report = run();
        assertThat(report.path("total").asInt()).isEqualTo(3);
        assertThat(report.path("passed").asInt()).isEqualTo(3);
        assertThat(report.at("/cases/0/actual/metrics/inputTokens").asInt()).isEqualTo(10);
        assertThat(report.at("/cases/0/actual/metrics/elapsedMs").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(report.at("/cases/2/actual/metrics/chatCalls").asInt()).isZero();
        assertThat(MODEL.chat().getRequests()).hasSize(2);
    }
    @Test void 출처가_유효해도_주장이_틀리면_평가에서_실패한다() throws Exception {
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("{\"answerable\":true,\"answer\":\"반품은 30일입니다.\",\"sourceIds\":[\"return-sealed\"]}"));
        var report = run();
        assertThat(report.path("passed").asInt()).isLessThan(3);
        assertThat(report.at("/cases/1/failures").toString()).contains("expected text missing");
    }
    @Test void 호출_제한은_실패로_기록하고_자동으로_재시도하지_않는다() throws Exception {
        MODEL
                .chat()
                .setHttpStatus(429);
        var report = run();
        assertThat(report.at("/cases/0/actual/reply/status").asString()).isEqualTo("FAILED");
        assertThat(report.at("/cases/0/actual/metrics/failureKind").asString()).isEqualTo("RATE_LIMIT");
        assertThat(MODEL.chat().getRequests()).hasSize(2);
    }
    @Test void 형식_오류를_실패로_기록하고_내부_예외는_노출하지_않는다() throws Exception {
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("not json"));
        var response = post("반품");
        assertThat(response.statusCode()).isEqualTo(200);
        var body = OpenAiStubServer.JSON.readTree(response.body());
        assertThat(body.at("/reply/status").asString()).isEqualTo("FAILED");
        assertThat(body.at("/metrics/failureKind").asString()).isEqualTo("UPSTREAM_OR_OUTPUT");
        assertThat(body.at("/metrics/inputTokens").isMissingNode() || body.at("/metrics/inputTokens").isNull()).isTrue();
    }
    @Test void 느린_응답은_시간_초과로_기록하고_자동으로_재시도하지_않는다() throws Exception {
        MODEL
                .chat()
                .setResponseDelay(java.time.Duration.ofMillis(1100));
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("고정 응답"));
        var response = post("반품");
        var body = OpenAiStubServer.JSON.readTree(response.body());
        assertThat(body.at("/reply/status").asString()).isEqualTo("FAILED");
        assertThat(body.at("/metrics/failureKind").asString()).isEqualTo("TIMEOUT");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        Thread.sleep(800);
    }
    tools.jackson.databind.JsonNode run() throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            var response = output.printHttpResponse(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/evaluation/run"))
                                    .POST(HttpRequest.BodyPublishers.noBody())
                                    .build(), HttpResponse.BodyHandlers.ofString()));
            assertThat(response.statusCode()).isEqualTo(200);
            return OpenAiStubServer.JSON.readTree(response.body());
        }
    }
    HttpResponse<String> post(String question) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return output.printHttpResponse(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/evaluation/ask"))
                                    .header("Content-Type", "text/plain; charset=UTF-8")
                                    .POST(HttpRequest.BodyPublishers.ofString(question))
                                    .build(), HttpResponse.BodyHandlers.ofString()));
        }
    }

    private static OpenAiStubServer createModelServer() {
        var server = new OpenAiStubServer();
        server
                .embeddings()
                .setVectorFunction(text -> {
                    if (text.contains("배송") || text.contains("운송")) return new float[]{1, 0, 0};
                    if (text.contains("반품") || text.contains("개봉")) return new float[]{0, 1, 0};
                    return new float[]{0, 0, 1};
                });
        server
                .chat()
                .setTokenUsage(10, 5);
        return server;
    }
}
