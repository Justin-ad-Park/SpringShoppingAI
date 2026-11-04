package com.example.ailearning.l05tools;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class ToolCallingContractTest {
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
    static LocalModelServer start() {
        try { return new LocalModelServer(); } catch (Exception error) { throw new IllegalStateException(error); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::url);
    }
    @BeforeEach void reset() { MODEL.requests.clear(); }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void Java로_직접_상품을_검색하면_모델을_호출하지_않는다() throws Exception {
        var response = get("/direct?category=%EC%9D%8C%EB%A3%8C&maxPrice=15000");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("P01").doesNotContain("P02");
        assertThat(MODEL.requests).isEmpty();
    }
    @Test void 모델이_요청한_도구를_실행하고_결과를_다시_전달한다() throws Exception {
        AtomicInteger turn = new AtomicInteger();
        MODEL.responder = request -> turn.incrementAndGet() == 1
                ? LocalModelServer.tool("search-1", "searchProducts", "{\"category\":\"음료\",\"maxPrice\":15000}")
                : LocalModelServer.message("허브티 12000원입니다.");
        var response = post("15000원 이하 음료");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(MODEL.requests).hasSize(2);
        assertThat(MODEL.requests.getFirst().path("tools").toString()).contains("searchProducts", "calculateShipping");
        String messages = MODEL.requests.get(1).path("messages").toString();
        assertThat(messages).contains("tool_call_id", "search-1", "P01").doesNotContain("P02");
    }
    @Test void 배송비는_상품_목록의_실제_가격으로_계산한다() throws Exception {
        AtomicInteger turn = new AtomicInteger();
        MODEL.responder = request -> turn.incrementAndGet() == 1
                ? LocalModelServer.tool("quote-1", "calculateShipping", "{\"productId\":\"P02\"}")
                : LocalModelServer.message("총 31000원입니다.");
        assertThat(post("커피 배송비 포함 가격").statusCode()).isEqualTo(200);
        assertThat(MODEL.requests.get(1).path("messages").toString()).contains("31000", "3000");
    }
    @Test void 인사는_도구를_호출하지_않고_답할_수_있다() throws Exception {
        MODEL.responder = request -> LocalModelServer.message("안녕하세요.");
        assertThat(post("안녕").body()).isEqualTo("안녕하세요.");
        assertThat(MODEL.requests).hasSize(1);
    }
    @Test void 상품_서비스가_조건과_상품_ID를_검증한다() {
        var catalog = new ProductCatalog();
        결과출력("전체 상품", catalog.search(null, null));
        결과출력("텀블러 배송비와 총액", catalog.quote("P03"));
        assertThat(catalog.search(null, null)).hasSize(3);
        assertThatThrownBy(() -> catalog.search("전자", 1000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.search(null, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.quote("invented")).isInstanceOf(IllegalArgumentException.class);
        assertThat(catalog.quote("P03").shippingFee()).isZero();
    }
    @Test void 공백_질문은_모델을_호출하지_않는다() throws Exception {
        assertThat(post(" ").statusCode()).isEqualTo(400);
        assertThat(MODEL.requests).isEmpty();
    }
    HttpResponse<String> get(String path) throws Exception {
        return send(HttpRequest.newBuilder(uri(path)).GET().build());
    }
    HttpResponse<String> post(String question) throws Exception {
        return send(HttpRequest.newBuilder(uri("/chat"))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(question)).build());
    }
    URI uri(String path) { return URI.create("http://127.0.0.1:" + port + "/api/tools" + path); }
    HttpResponse<String> send(HttpRequest request) throws Exception {
        try (var http = HttpClient.newHttpClient()) { return 응답출력(http.send(request, HttpResponse.BodyHandlers.ofString())); }
    }
}
