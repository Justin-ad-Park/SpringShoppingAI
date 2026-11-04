package com.example.ailearning.l10rag;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.URI;
import java.net.http.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class RagContractTest {
    private String 테스트이름;

    @BeforeEach
    void 테스트_이름_준비(TestInfo info) {
        테스트이름 = info.getTestMethod().orElseThrow().getName();
    }

    private void 결과출력(String 항목, Object 결과) {
        System.out.printf("[%s] %s: %s%n", 테스트이름, 항목, 결과);
    }

    private HttpResponse<String> 응답출력(HttpResponse<String> response) {
        결과출력("요청", response.request().method() + " " + response.uri().getPath());
        결과출력("HTTP " + response.statusCode(), response.body());
        return response;
    }

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
    @BeforeEach void reset() { MODEL.requests.clear(); }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void 문서를_나누어_검색한_근거를_모델에_전달한다() throws Exception {
        MODEL.responder = request -> ModelWireServer.message("{\"answerable\":true,\"answer\":\"미개봉은 7일 이내입니다.\",\"sourceIds\":[\"return-sealed\"]}");
        var response = post("미개봉 반품 기간");
        assertThat(response.statusCode()).isEqualTo(200);
        var body = ModelWireServer.JSON.readTree(response.body());
        assertThat(body.path("status").asString()).isEqualTo("ANSWERED");
        assertThat(body.path("sources").get(0).path("location").asString()).isEqualTo("policies.md#return-sealed");
        String messages = MODEL.requests.getFirst().path("messages").toString();
        assertThat(messages).contains("REFERENCE_START", "return-sealed", "7일", "return-opened");
        assertThat(MODEL.embeddingRequests.toString()).contains("미개봉", "반품");
    }
    @Test void 검색_근거가_없으면_답변_모델을_호출하지_않는다() throws Exception {
        var response = post("우주 여행");
        assertThat(ModelWireServer.JSON.readTree(response.body()).path("status").asString()).isEqualTo("NO_CONTEXT");
        assertThat(MODEL.requests).isEmpty();
    }
    @Test void 검색하지_않은_출처나_출처_누락은_답변으로_채택하지_않는다() throws Exception {
        MODEL.responder = request -> ModelWireServer.message("{\"answerable\":true,\"answer\":\"아무 주장\",\"sourceIds\":[\"invented\"]}");
        assertThat(ModelWireServer.JSON.readTree(post("반품").body()).path("status").asString()).isEqualTo("INVALID_CITATION");
        MODEL.responder = request -> ModelWireServer.message("{\"answerable\":true,\"answer\":\"아무 주장\",\"sourceIds\":[]}");
        assertThat(ModelWireServer.JSON.readTree(post("반품").body()).path("status").asString()).isEqualTo("INVALID_CITATION");
    }
    @Test void 문서를_찾았어도_근거가_부족하면_답변을_보류한다() throws Exception {
        MODEL.responder = request -> ModelWireServer.message("{\"answerable\":false,\"answer\":\"모릅니다\",\"sourceIds\":[]}");
        assertThat(ModelWireServer.JSON.readTree(post("반품 담당자 이름").body()).path("status").asString()).isEqualTo("NO_ANSWER");
    }
    @Test void 답변_형식이_잘못되면_변환_실패로_처리한다() throws Exception {
        MODEL.responder = request -> ModelWireServer.message("형식 오류");
        assertThat(post("반품").statusCode()).isEqualTo(502);
    }
    @Test void 공백_질문은_답변_모델을_호출하지_않는다() throws Exception {
        assertThat(post(" ").statusCode()).isEqualTo(400);
        assertThat(MODEL.requests).isEmpty();
    }
    HttpResponse<String> post(String question) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return 응답출력(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/rag"))
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(question)).build(), HttpResponse.BodyHandlers.ofString()));
        }
    }
}
