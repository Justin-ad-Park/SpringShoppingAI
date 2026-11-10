package com.example.ailearning.l13intent;

import com.example.ailearning.testsupport.OpenAiStubServer;
import com.example.ailearning.testsupport.TestNamePrinter;
import com.example.ailearning.testsupport.TestOutput;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantTextMessage;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class IntentClassificationLocalModelTest {
    private static final OpenAiStubServer MODEL = new OpenAiStubServer();
    private final TestOutput output = new TestOutput();

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
    void 대분류_선택_뒤에는_해당_소분류만_후보로_전달한다() throws Exception {
        // Given: 첫 응답은 대분류, 두 번째 응답은 그 아래 소분류다.
        MODEL
                .chat()
                .setResponseSequence(
                        createAssistantTextMessage(createChoiceJson("MATCH", "RETURN")),
                        createAssistantTextMessage(createChoiceJson("MATCH", "RETURN.FEE")));

        // When: 실제 Controller와 Spring AI를 통해 두 단계 분류 실행
        var response = post("미개봉 상품을 반품하면 배송비는 누가 내나요?");

        // Then: 응답 코드와 실제 두 모델 요청의 후보 범위를 확인한다.
        assertThat(response.statusCode()).isEqualTo(200);
        var body = OpenAiStubServer.JSON.readTree(response.body());
        assertThat(body.path("decision").asString()).isEqualTo("MATCH");
        assertThat(body.path("majorCode").asString()).isEqualTo("RETURN");
        assertThat(body.path("minorCode").asString()).isEqualTo("RETURN.FEE");
        assertThat(body.path("taxonomyVersion").asString()).isEqualTo("shopping-intents-v1");
        assertThat(MODEL.chat().getRequests()).hasSize(2);
        var requests = MODEL.chat().getRequests();
        String first = requests.getFirst().path("messages").get(0).path("content").asString();
        String second = requests.get(1).path("messages").get(0).path("content").asString();
        assertThat(first).contains("PRODUCT:", "RETURN:", "SUPPORT:").doesNotContain("RETURN.FEE:");
        assertThat(second).contains("RETURN.REQUEST:", "RETURN.FEE:", "RETURN.PICKUP:", "RETURN.WITHDRAW:")
                .doesNotContain("\nORDER.CANCEL:", "\nREFUND.TIMING:");
        assertThat(MODEL.chat().getRequests().get(1).path("messages").toString()).contains("미개봉 상품을 반품하면");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLARIFY", "OUT_OF_SCOPE", "MULTIPLE"})
    void 대분류를_확정하지_못하면_소분류_모델을_호출하지_않는다(String decision) throws Exception {
        // Given: 모호함·범위 밖·복수 요청 중 하나를 모델이 반환
        MODEL
                .chat()
                .setResponseText(createChoiceJson(decision, null));

        // When: 실제 API로 분류 요청
        var response = post("분류할 문의");

        // Then: 미확정 결과에 임의의 분류 코드를 채우지 않는다.
        assertThat(response.statusCode()).isEqualTo(200);
        var body = OpenAiStubServer.JSON.readTree(response.body());
        assertThat(body.path("decision").asString()).isEqualTo(decision);
        assertThat(body.path("majorCode").isNull()).isTrue();
        assertThat(body.path("minorCode").isNull()).isTrue();
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLARIFY", "OUT_OF_SCOPE", "MULTIPLE"})
    void 소분류를_확정하지_못하면_대분류만_남기고_처리를_보류한다(String decision) throws Exception {
        MODEL
                .chat()
                .setResponseSequence(
                        createAssistantTextMessage(createChoiceJson("MATCH", "RETURN")),
                        createAssistantTextMessage(createChoiceJson(decision, null)));

        var response = post("반품 문의");

        assertThat(response.statusCode()).isEqualTo(200);
        var body = OpenAiStubServer.JSON.readTree(response.body());
        assertThat(body.path("decision").asString()).isEqualTo(decision);
        assertThat(body.path("majorCode").asString()).isEqualTo("RETURN");
        assertThat(body.path("minorCode").isNull()).isTrue();
        assertThat(MODEL.chat().getRequests()).hasSize(2);
    }

    @Test
    void 등록되지_않은_대분류는_거부하고_두번째_호출을_하지_않는다() throws Exception {
        MODEL
                .chat()
                .setResponseText(createChoiceJson("MATCH", "INVENTED"));

        assertThat(post("문의").statusCode()).isEqualTo(502);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    @Test
    void 다른_대분류의_소분류는_실제로_등록되어_있어도_거부한다() throws Exception {
        // Given: REFUND.TIMING은 유효한 코드지만 RETURN의 자식은 아니다.
        MODEL
                .chat()
                .setResponseSequence(
                        createAssistantTextMessage(createChoiceJson("MATCH", "RETURN")),
                        createAssistantTextMessage(createChoiceJson("MATCH", "REFUND.TIMING")));

        assertThat(post("반품 문의").statusCode()).isEqualTo(502);
        assertThat(MODEL.chat().getRequests()).hasSize(2);
    }

    @Test
    void 미확정_판단에_분류_코드를_섞은_모델_응답은_거부한다() throws Exception {
        MODEL
                .chat()
                .setResponseText(createChoiceJson("CLARIFY", "RETURN"));

        assertThat(post("애매한 문의").statusCode()).isEqualTo(502);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    @Test
    void 깨진_JSON은_업무_분류_성공으로_처리하지_않는다() throws Exception {
        MODEL
                .chat()
                .setResponseText("JSON이 아닌 답변");

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

    private String createChoiceJson(String decision, String code) {
        var json = OpenAiStubServer.JSON.createObjectNode().put("decision", decision);
        if (code == null) {
            json.putNull("code");
        } else {
            json.put("code", code);
        }
        return json.toString();
    }

    private HttpResponse<String> post(String question) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/intents"))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(question))
                .build();
        try (var http = HttpClient.newHttpClient()) {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return output.printHttpResponse(response);
        }
    }
}
