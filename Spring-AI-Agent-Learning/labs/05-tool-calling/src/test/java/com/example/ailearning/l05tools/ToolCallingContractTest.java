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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ToolCallingContractTest {
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

    @Test void directJavaCallDoesNotContactModel() throws Exception {
        var response = get("/direct?category=%EC%9D%8C%EB%A3%8C&maxPrice=15000");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("P01").doesNotContain("P02");
        assertThat(MODEL.requests).isEmpty();
    }
    @Test void modelRequestsToolApplicationExecutesAndSendsResultBack() throws Exception {
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
    @Test void shippingUsesCatalogPriceRatherThanInventedModelPrice() throws Exception {
        AtomicInteger turn = new AtomicInteger();
        MODEL.responder = request -> turn.incrementAndGet() == 1
                ? LocalModelServer.tool("quote-1", "calculateShipping", "{\"productId\":\"P02\"}")
                : LocalModelServer.message("총 31000원입니다.");
        assertThat(post("커피 배송비 포함 가격").statusCode()).isEqualTo(200);
        assertThat(MODEL.requests.get(1).path("messages").toString()).contains("31000", "3000");
    }
    @Test void greetingCanFinishWithoutToolCall() throws Exception {
        MODEL.responder = request -> LocalModelServer.message("안녕하세요.");
        assertThat(post("안녕").body()).isEqualTo("안녕하세요.");
        assertThat(MODEL.requests).hasSize(1);
    }
    @Test void javaValidatesOptionalConditionsAndUnknownProducts() {
        var catalog = new ProductCatalog();
        assertThat(catalog.search(null, null)).hasSize(3);
        assertThatThrownBy(() -> catalog.search("전자", 1000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.search(null, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.quote("invented")).isInstanceOf(IllegalArgumentException.class);
        assertThat(catalog.quote("P03").shippingFee()).isZero();
    }
    @Test void blankInputDoesNotContactModel() throws Exception {
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
        try (var http = HttpClient.newHttpClient()) { return http.send(request, HttpResponse.BodyHandlers.ofString()); }
    }
}
