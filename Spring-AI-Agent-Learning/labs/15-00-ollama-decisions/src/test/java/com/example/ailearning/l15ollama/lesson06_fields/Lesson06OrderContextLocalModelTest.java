package com.example.ailearning.l15ollama.lesson06_fields;

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
import static org.assertj.core.api.Assertions.assertThat;

/** 실제 Ollama 대신 고정 HTTP 응답을 사용해 Starter·DecisionModel·MVC 연결을 확인한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Lesson06OrderContextLocalModelTest {
    private static final String DECISION_PATH = "/v1/systemone";
    private static final StubHttpServer MODEL = new StubHttpServer();

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
    void 고객_문장과_주문_필드를_순서를_유지한_JSON으로_전달한다() throws Exception {
        respondWithAnswers("""
                "next_action":{"type":"choice","choice":"EXPLAIN_RETURN","confidence":0.8,
                  "probabilities":{"ORDER_PAYMENT":0.05,"EXPLAIN_RETURN":0.9,"OTHER_QUESTION":0.05}}
                """);
        var response = appClient.postJson("/api/ollama/order-context", """
                {"message":"받은 상품을 반품하고 싶어요.","orderStatus":"DELIVERED","daysSinceDelivery":2}
                """);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(StubHttpServer.JSON.readTree(response.body()).path("nextAction").asString()).isEqualTo("EXPLAIN_RETURN");
        var state = MODEL.getRequestBodies(DECISION_PATH).getFirst().path("state");
        System.out.println("이름 붙은 입력 필드: " + state.toPrettyString());
        assertThat(state.path("orderStatus").asString()).isEqualTo("DELIVERED");
        assertThat(state.path("daysSinceDelivery").asInt()).isEqualTo(2);
        assertThat(state.properties().stream().map(java.util.Map.Entry::getKey).toList()).containsExactly("message", "orderStatus", "daysSinceDelivery");
    }

    private void respondWithAnswers(String answers) {
        var body = StubHttpServer.JSON.readTree("""
                {"model":"learning-decision-model","answers":{%s},"usage":{"input_tokens":20,"output_tokens":1}}
                """.formatted(answers));
        MODEL.registerResponseHandler(DECISION_PATH, request -> new StubHttpServer.Response(200, body, 0));
    }

}
