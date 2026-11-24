package com.example.ailearning.l14jev.lesson01_choice;

import com.example.ailearning.l14jev.JevStubServer;

import com.example.ailearning.testsupport.http.LocalTestHttpClient;
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
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Lesson01JevChoiceLocalModelTest {
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
        JsonNode decisionJson = post("intent", "3만원 이하의 텀블러를 추천해 주세요.");
        assertThat(decisionJson.path("predictedIntent").asString()).isEqualTo("PRODUCT_SEARCH");
        assertThat(decisionJson.path("clarificationRequired").asBoolean()).isFalse();
        assertThat(decisionJson.path("probabilities").size()).isEqualTo(4);
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
        HttpResponse<String> response = appClient.postText("/api/jev/" + endpoint, message);
        assertThat(response.statusCode()).isEqualTo(200);
        return JevStubServer.JSON.readTree(response.body());
    }
}
