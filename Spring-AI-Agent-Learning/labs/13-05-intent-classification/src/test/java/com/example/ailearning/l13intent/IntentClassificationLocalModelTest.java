package com.example.ailearning.l13intent;

import com.example.ailearning.testsupport.OpenAiStubServer;
import com.example.ailearning.testsupport.TestNamePrinter;
import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.LocalTestHttpClient;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class IntentClassificationLocalModelTest {
    private static final OpenAiStubServer MODEL = new OpenAiStubServer();
    private final TestOutput output = new TestOutput();

    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> this.port, output);

    @Value("${local.server.port}")
    private int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        MODEL
                .chat()
                .resetResponsesAndRequests();
        TestNamePrinter.printTestName(testInfo);
    }

    @AfterAll
    static void 테스트_종료() {
        MODEL.close();
    }

    @Test
    void 전체_의도_트리와_출력_예시를_한번_전달해_대분류와_소분류를_함께_받는다() throws Exception {
        // Given: 의미 판단은 고정하고, 실제 전송 내용과 응답 변환을 검증한다.
        MODEL
                .chat()
                .setResponseText(createPredictionJson("MATCH", "RETURN", "FEE"));

        // When
        var response = post("미개봉 상품의 반품 배송비 부담 기준이 궁금합니다.");

        // Then: 한 번의 모델 응답을 API 결과로 변환한다.
        assertThat(response.statusCode()).isEqualTo(200);
        var body = OpenAiStubServer.JSON.readTree(response.body());
        assertThat(body.path("decision").asString()).isEqualTo("MATCH");
        assertThat(body.path("main_intent").asString()).isEqualTo("RETURN");
        assertThat(body.path("sub_intent").asString()).isEqualTo("FEE");
        assertThat(body.path("taxonomyVersion").asString()).isEqualTo("shopping-intents-v2");
        assertThat(MODEL.chat().getRequests()).hasSize(1);

        var messages = MODEL.chat().getRequests().getFirst().path("messages");
        String system = messages.get(0).path("content").asString();
        assertThat(system).contains("입력·출력 예시:", "반품 배송비 부담 기준", "DELIVERY", "TRACK");
        assertThat(system).doesNotContain("{intents_json}");
        assertThat(messages.toString()).contains("미개봉 상품의 반품 배송비 부담 기준");

        // #### 사이에 JSON 트리 전체가 들어갔으며 각 소분류의 소속 관계가 유지된다.
        int start = system.indexOf("####") + 4;
        int end = system.lastIndexOf("####");
        var tree = OpenAiStubServer.JSON.readTree(system.substring(start, end));
        assertThat(tree.size()).isEqualTo(10);
        var returns = findMainIntent(tree, "RETURN").path("sub_intents");
        assertThat(returns.size()).isEqualTo(4);
        assertThat(returns.get(1).path("sub_intent").asString()).isEqualTo("FEE");
        assertThat(findMainIntent(tree, "REFUND").path("sub_intents").toString()).contains("TIMING");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLARIFY", "OUT_OF_SCOPE", "MULTIPLE"})
    void 미확정_문의는_분류_코드_없이_안내하고_추가_모델_요청을_하지_않는다(String decision) throws Exception {
        // Given: 보류 판단도 모델의 추정이므로 이 테스트에서는 응답으로 고정한다.
        MODEL
                .chat()
                .setResponseText(createPredictionJson(decision, null, null));

        var response = post("분류할 문의");

        assertThat(response.statusCode()).isEqualTo(200);
        var body = OpenAiStubServer.JSON.readTree(response.body());
        assertThat(body.path("decision").asString()).isEqualTo(decision);
        assertThat(body.path("main_intent").isNull()).isTrue();
        assertThat(body.path("sub_intent").isNull()).isTrue();
        assertThat(body.path("message").asString()).isNotBlank();
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource({"INVENTED,FEE", "RETURN,INVENTED", "RETURN,TIMING"})
    void 미등록_코드나_다른_대분류의_소분류_조합을_거부한다(String main, String sub) throws Exception {
        // RETURN:TIMING은 두 코드가 각각 존재해도 같은 가지에 없는 조합이다.
        MODEL
                .chat()
                .setResponseText(createPredictionJson("MATCH", main, sub));

        assertThat(post("반품 문의").statusCode()).isEqualTo(502);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "JSON이 아닌 답변",
            "{\"decision\":null,\"main_intent\":null,\"sub_intent\":null}",
            "{\"decision\":\"MATCH\",\"main_intent\":\"RETURN\",\"sub_intent\":null}",
            "{\"decision\":\"CLARIFY\",\"main_intent\":\"RETURN\",\"sub_intent\":null}",
            "{\"decision\":\"UNKNOWN\",\"main_intent\":null,\"sub_intent\":null}"
    })
    void 형식이나_판단과_코드의_규칙을_어긴_응답은_성공으로_처리하지_않는다(String responseText) throws Exception {
        MODEL
                .chat()
                .setResponseText(responseText);

        assertThat(post("문의").statusCode()).isEqualTo(502);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    @Test
    void 모델_통신_실패는_분류_보류와_구분한다() throws Exception {
        MODEL
                .chat()
                .setHttpStatus(503);

        assertThat(post("문의").statusCode()).isEqualTo(502);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    @Test
    void 공백_질문은_모델을_호출하기_전에_거부한다() throws Exception {
        assertThat(post(" ").statusCode()).isEqualTo(400);
        assertThat(MODEL.chat().getRequests()).isEmpty();
    }

    private JsonNode findMainIntent(JsonNode tree, String code) {
        for (var main : tree) {
            if (main.path("main_intent").asString().equals(code)) {
                return main;
            }
        }
        throw new AssertionError("의도 트리에 대분류가 없음: " + code);
    }

    private String createPredictionJson(String decision, String main, String sub) {
        return OpenAiStubServer.JSON.createObjectNode()
                .put("decision", decision)
                .put("main_intent", main)
                .put("sub_intent", sub)
                .toString();
    }

    private HttpResponse<String> post(String question) throws Exception {
        return appClient.postText("/api/intents", question);
    }
}
