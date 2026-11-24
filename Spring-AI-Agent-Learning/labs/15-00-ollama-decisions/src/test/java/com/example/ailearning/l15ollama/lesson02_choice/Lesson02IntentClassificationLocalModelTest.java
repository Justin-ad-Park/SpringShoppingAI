package com.example.ailearning.l15ollama.lesson02_choice;

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
class Lesson02IntentClassificationLocalModelTest {
    private static final String DECISION_PATH = "/v1/systemone";
    private static final StubHttpServer MODEL = new StubHttpServer();
    private static final String INTENT_ANSWER = """
            "intent":{"type":"choice","choice":"PRODUCT_SEARCH","confidence":0.85,
              "probabilities":{"PRODUCT_SEARCH":0.9,"ORDER_DELIVERY":0.04,"RETURN_EXCHANGE":0.03,"OTHER":0.03}}
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
    void Choice로_쇼핑_의도와_항목별_확률을_받는다() throws Exception {
        respondWithAnswers(INTENT_ANSWER);
        var result = postText("intent", "3만원 이하 텀블러를 추천해 주세요.");
        assertThat(result.path("predictedIntent").asString()).isEqualTo("PRODUCT_SEARCH");
        assertThat(result.path("clarificationRequired").asBoolean()).isFalse();
        assertThat(result.path("probabilities").size()).isEqualTo(4);
        var request = MODEL.getRequestBodies(DECISION_PATH).getFirst();
        System.out.println("Ollama에 전송한 요청: " + request.toPrettyString());
        assertThat(request.path("state").asString()).contains("텀블러");
        assertThat(request.path("model").asString()).isEqualTo("learning-decision-model");
        assertThat(request.path("questions").path("intent").path("type").asString()).isEqualTo("choice");
        assertThat(request.path("questions").path("intent").path("criteria").size()).isEqualTo(4);
    }

    @Test
    void 낮은_confidence의_의도는_확인_질문으로_보류한다() throws Exception {
        respondWithAnswers(INTENT_ANSWER.replace("0.85", "0.2"));
        assertThat(postText("intent", "그건 어떻게 됐나요?").path("clarificationRequired").asBoolean()).isTrue();
    }

    @Test
    void 공백_메시지는_모델_호출_전에_400으로_거부한다() throws Exception {
        assertThat(appClient.postText("/api/ollama/intent", "   ").statusCode()).isEqualTo(400);
        assertThat(MODEL.getRequestBodies(DECISION_PATH)).isEmpty();
    }

    @Test
    void 모델의_오류를_정상_분류로_대체하지_않는다() throws Exception {
        // 기본 대역은 모델의 HTTP 400 오류를 반환한다. 우리 API는 미처리 실패를 500으로 반환한다.
        assertThat(appClient.postText("/api/ollama/intent", "상품을 추천해 주세요.").statusCode()).isEqualTo(500);
        assertThat(MODEL.getRequestBodies(DECISION_PATH)).hasSize(1);
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
