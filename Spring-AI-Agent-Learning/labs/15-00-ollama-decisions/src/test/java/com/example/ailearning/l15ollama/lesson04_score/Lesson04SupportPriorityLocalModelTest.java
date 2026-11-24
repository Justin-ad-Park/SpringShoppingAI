package com.example.ailearning.l15ollama.lesson04_score;

import com.example.ailearning.testsupport.http.LocalTestHttpClient;
import com.example.ailearning.testsupport.model.internal.StubHttpServer;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import com.example.ailearning.testsupport.output.TestOutput;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import static org.assertj.core.api.Assertions.assertThat;

/** 실제 Ollama 대신 고정 HTTP 응답을 사용해 Starter·DecisionModel·MVC 연결을 확인한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Lesson04SupportPriorityLocalModelTest {
    private static final String DECISION_PATH = "/v1/systemone";
    private static final StubHttpServer MODEL = new StubHttpServer();

    private static final String URGENCY_ANSWER = """
            "urgency":{"type":"score","score":1.6,"confidence":0.8,
              "probabilities":{"0":0.1,"1":0.2,"2":0.7}}
            """;

    @Value("${local.server.port}") int port;
    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> port, new TestOutput());

    @DynamicPropertySource
    static void modelAddress(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.ollama.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL.clearRecordedRequests(DECISION_PATH);
        MODEL.registerResponseHandler(DECISION_PATH,
                request -> new StubHttpServer.Response(400, StubHttpServer.createErrorResponseBody("response_not_configured"), 0));
    }

    @AfterAll
    static void 모델_대역_종료() {
        MODEL.close();
    }

    @Test
    void Score의_소수_점수와_대표_단계를_구분한다() throws Exception {
        respondWithAnswers(URGENCY_ANSWER);
        var result = postText("priority", "취소 마감이 한 시간 뒤입니다.");
        assertThat(result.path("score").asDouble()).isEqualTo(1.6);
        assertThat(result.path("nearestLevel").asInt()).isEqualTo(2);
        assertThat(result.path("priority").asString()).isEqualTo("HIGH");
        assertThat(result.path("probabilities").size()).isEqualTo(3);
        var question = MODEL.getRequestBodies(DECISION_PATH).getFirst().path("questions").path("urgency");
        assertThat(question.path("criteria").size()).isEqualTo(3);
    }

    private void respondWithAnswers(String answers) {
        var body = StubHttpServer.JSON.readTree("""
                {"model":"learning-decision-model","answers":{%s},"usage":{"input_tokens":20,"output_tokens":1}}
                """.formatted(answers));
        MODEL.registerResponseHandler(DECISION_PATH, request -> new StubHttpServer.Response(200, body, 0));
    }

    private JsonNode postText(String endpoint, String message) throws Exception {
        var response = appClient.postText("/api/ollama/" + endpoint, message);
        assertThat(response.statusCode()).isEqualTo(200);
        return StubHttpServer.JSON.readTree(response.body());
    }
}
