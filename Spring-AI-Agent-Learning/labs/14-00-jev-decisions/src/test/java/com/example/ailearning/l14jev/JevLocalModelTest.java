package com.example.ailearning.l14jev;

import com.example.ailearning.testsupport.http.LocalTestHttpClient;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import com.example.ailearning.testsupport.output.TestOutput;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class JevLocalModelTest {
    private static final JevStubServer MODEL = new JevStubServer();
    @Value("${local.server.port}") private int port;
    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> port, new TestOutput());

    @DynamicPropertySource
    static void modelAddress(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.typesafe.base-url", MODEL::baseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL.reset();
    }

    @AfterAll
    static void 모델_대역_종료() {
        MODEL.close();
    }

    @Test
    void Choice로_의도를_분류하고_낮은_신뢰도는_확인_질문으로_보류한다() throws Exception {
        respond("intent", """
                {"type":"choice","choice":"PRODUCT_SEARCH","confidence":0.85,
                 "probabilities":{"PRODUCT_SEARCH":0.9,"ORDER_DELIVERY":0.04,"RETURN_EXCHANGE":0.03,"OTHER":0.03}}
                """);
        var body = post("intent", "3만원 이하의 텀블러를 추천해 주세요.");
        assertThat(body.path("predictedIntent").asString()).isEqualTo("PRODUCT_SEARCH");
        assertThat(body.path("clarificationRequired").asBoolean()).isFalse();
        assertThat(body.path("probabilities").size()).isEqualTo(4);
        JsonNode request = MODEL.requests().getFirst();
        assertThat(request.path("state").asString()).contains("텀블러를 추천");
        assertThat(request.path("model").asString()).isEqualTo("learning-jev");
        assertThat(request.path("questions").path("intent").path("type").asString()).isEqualTo("choice");
        assertThat(request.path("questions").path("intent").path("criteria").size()).isEqualTo(4);
        assertThat(MODEL.authorization()).isEqualTo("Bearer local-test-key");

        respond("intent", """
                {"type":"choice","choice":"ORDER_DELIVERY","confidence":0.2,
                 "probabilities":{"PRODUCT_SEARCH":0.3,"ORDER_DELIVERY":0.4,"RETURN_EXCHANGE":0.2,"OTHER":0.1}}
                """);
        assertThat(post("intent", "그건 어떻게 됐나요?").path("clarificationRequired").asBoolean()).isTrue();
        assertThat(MODEL.requests()).hasSize(2);
    }

    @Test
    void Score는_소수_점수와_신뢰도를_받고_Java가_상담_우선순위를_정한다() throws Exception {
        respond("urgency", """
                {"type":"score","score":1.6,"confidence":0.8,
                 "legend":{"0":"일반 문의","1":"현재 문제","2":"즉시 확인 필요"},
                 "probabilities":{"0":0.1,"1":0.2,"2":0.7}}
                """);
        var body = post("priority", "취소 마감이 오늘인데 아직 처리가 안 됐어요.");
        assertThat(body.path("score").asDouble()).isEqualTo(1.6);
        assertThat(body.path("priority").asString()).isEqualTo("HIGH");
        var question = MODEL.requests().getFirst().path("questions").path("urgency");
        assertThat(question.path("type").asString()).isEqualTo("score");
        assertThat(question.path("criteria").isArray()).isTrue();
        assertThat(question.path("criteria").size()).isEqualTo(3);
    }

    @Test
    void Noul은_참일_확률을_받아_연결_안내_또는_확인_질문을_선택한다() throws Exception {
        respond("human_support", "{\"type\":\"noul\",\"noul\":0.9}");
        var body = post("human-support", "자동 응답 말고 사람 상담원과 이야기하고 싶어요.");
        assertThat(body.path("yesProbability").asDouble()).isEqualTo(0.9);
        assertThat(body.path("action").asString()).isEqualTo("OFFER_HUMAN_SUPPORT");
        var question = MODEL.requests().getFirst().path("questions").path("human_support");
        assertThat(question.path("type").asString()).isEqualTo("noul");
        assertThat(question.path("criteria").has("true")).isTrue();
        assertThat(body.has("confidence")).isFalse();

        respond("human_support", "{\"type\":\"noul\",\"noul\":0.5}");
        assertThat(post("human-support", "이 문의는 어떻게 해야 하나요?").path("action").asString())
                .isEqualTo("ASK_CLARIFICATION");
    }

    @ParameterizedTest
    @ValueSource(strings = {"intent", "priority", "human-support"})
    void 공백_입력은_400이며_Jev를_호출하지_않는다(String endpoint) throws Exception {
        assertThat(appClient.postText("/api/jev/" + endpoint, "   ").statusCode()).isEqualTo(400);
        assertThat(MODEL.requests()).isEmpty();
    }

    @Test
    void 모델_장애를_정상_의도로_대체하지_않고_재시도_없이_실패한다() throws Exception {
        MODEL.respond(503, "{\"error\":\"unavailable\"}");
        assertThat(appClient.postText("/api/jev/intent", "주문한 상품이 어디 있나요?").statusCode()).isEqualTo(500);
        assertThat(MODEL.requests()).hasSize(1);
    }

    @Test
    void 응답_타입이_요청과_다르면_판정_결과를_반환하지_않는다() throws Exception {
        respond("intent", "{\"type\":\"noul\",\"noul\":0.9}");
        assertThat(appClient.postText("/api/jev/intent", "텀블러를 추천해 주세요.").statusCode()).isEqualTo(500);
    }

    private void respond(String name, String answerJson) {
        MODEL.respond(200, "{\"model\":\"learning-jev\",\"answers\":{\"" + name + "\":" + answerJson + "}}");
    }

    private JsonNode post(String endpoint, String message) throws Exception {
        var response = appClient.postText("/api/jev/" + endpoint, message);
        assertThat(response.statusCode()).isEqualTo(200);
        return JevStubServer.JSON.readTree(response.body());
    }
}
