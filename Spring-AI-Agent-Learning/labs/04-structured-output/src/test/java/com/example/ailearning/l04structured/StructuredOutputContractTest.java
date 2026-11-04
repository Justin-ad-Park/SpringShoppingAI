package com.example.ailearning.l04structured;

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
class StructuredOutputContractTest {
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

    static final LocalModelServer MODEL = start();
    @Value("${local.server.port}") int port;
    @org.springframework.beans.factory.annotation.Autowired CriteriaService service;

    static LocalModelServer start() {
        try { return new LocalModelServer(); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::url);
    }
    @BeforeEach void reset() { MODEL.requests.clear(); }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void JSON_응답을_record로_변환하고_형식_지시를_전달한다() throws Exception {
        MODEL.responder = request -> LocalModelServer.message("{\"category\":\"음료\",\"maxPrice\":15000}");
        var response = post("음료 15000원 이하");
        assertThat(response.statusCode()).isEqualTo(200);
        var body = LocalModelServer.JSON.readTree(response.body());
        assertThat(body.path("category").asString()).isEqualTo("음료");
        assertThat(body.path("maxPrice").asInt()).isEqualTo(15000);
        assertThat(MODEL.requests.getFirst().path("messages").toString()).contains("category", "maxPrice");
    }
    @Test void 사용자가_말하지_않은_조건은_null로_유지한다() throws Exception {
        MODEL.responder = request -> LocalModelServer.message("{\"category\":null,\"maxPrice\":null}");
        SearchCriteria criteria = service.extract("선물 추천");
        결과출력("조건 객체", criteria);
        assertThat(criteria.category()).isNull();
        assertThat(criteria.maxPrice()).isNull();
    }
    @Test void 잘못된_JSON_응답은_변환_실패로_처리한다() throws Exception {
        MODEL.responder = request -> LocalModelServer.message("이것은 JSON이 아닙니다.");
        assertThat(post("음료 추천").statusCode()).isEqualTo(502);
    }
    @Test void 변환에_성공해도_허용되지_않는_값은_거부한다() throws Exception {
        MODEL.responder = request -> LocalModelServer.message("{\"category\":\"음료\",\"maxPrice\":-1}");
        assertThat(post("음료 추천").statusCode()).isEqualTo(422);
        MODEL.responder = request -> LocalModelServer.message("{\"category\":\"전자\",\"maxPrice\":1000}");
        assertThat(post("추천").statusCode()).isEqualTo(422);
    }
    @Test void 공백_질문은_모델_호출_전에_거부한다() throws Exception {
        assertThat(post("   ").statusCode()).isEqualTo(400);
        assertThat(MODEL.requests).isEmpty();
    }
    HttpResponse<String> post(String question) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return 응답출력(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/criteria"))
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(question)).build(), HttpResponse.BodyHandlers.ofString()));
        }
    }
}
