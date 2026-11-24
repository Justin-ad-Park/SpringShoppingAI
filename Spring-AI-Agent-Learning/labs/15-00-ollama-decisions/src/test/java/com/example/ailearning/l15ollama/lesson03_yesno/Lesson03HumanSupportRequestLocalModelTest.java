package com.example.ailearning.l15ollama.lesson03_yesno;

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
class Lesson03HumanSupportRequestLocalModelTest {
    private static final String DECISION_PATH = "/v1/systemone";
    private static final StubHttpServer MODEL = new StubHttpServer();

    private static final String HUMAN_SUPPORT_ANSWER = """
            "human_support":{"type":"noul","noul":0.9}
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
    void YesNo는_noul로_전송되고_참일_확률을_반환한다() throws Exception {
        respondWithAnswers(HUMAN_SUPPORT_ANSWER);
        var result = postText("human-support", "사람 상담원에게 연결해 주세요.");
        assertThat(result.path("yesProbability").asDouble()).isEqualTo(0.9);
        assertThat(result.path("action").asString()).isEqualTo("OFFER_HUMAN_SUPPORT");
        var question = MODEL.getRequestBodies(DECISION_PATH).getFirst().path("questions").path("human_support");
        assertThat(question.path("type").asString()).isEqualTo("noul");
        assertThat(question.path("criteria").has("true")).isTrue();
        assertThat(result.has("confidence")).isFalse();
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
