package com.example.ailearning.l08advisors;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import java.net.*;
import java.net.http.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdvisorContractTest {
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
    @BeforeEach void reset() {
        MODEL.requests.clear();
        MODEL.responder = request -> LocalModelServer.message("고정 응답");
    }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void advisorsUnwindInReverseOrder() throws Exception {
        try (var browser = browser()) {
            var response = post(browser, "/api/advisors/demo", "안녕");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(json(response).path("steps").toString()).isEqualTo("[\"A:before\",\"B:before\",\"B:after\",\"A:after\"]");
        }
    }
    @Test void memoryAdvisorReplaysOnceWithoutManualDoubleSaving() throws Exception {
        try (var browser = browser()) {
            post(browser, "/api/advisors/demo", "예산은 3만원");
            var response = post(browser, "/api/advisors/demo", "내 예산은?");
            assertThat(json(response).path("storedMessages").asInt()).isEqualTo(4);
            assertThat(MODEL.requests.get(1).path("messages").toString()).contains("예산은 3만원");
            assertThat(MODEL.requests.get(1).path("messages").size()).isEqualTo(4);
        }
    }
    @Test void insideTraceSeesEachToolIterationOutsideTraceSeesWholeTurn() throws Exception {
        var turn = new java.util.concurrent.atomic.AtomicInteger();
        MODEL.responder = request -> turn.incrementAndGet() == 1
                ? LocalModelServer.tool("price", "getTeaPrice", "{}") : LocalModelServer.message("허브티 12000원");
        try (var browser = browser()) {
            var response = post(browser, "/api/advisors/tool?tools=true", "허브티 가격");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(json(response).path("steps").toString()).isEqualTo(
                    "[\"A:before\",\"B:before\",\"B:after\",\"B:before\",\"B:after\",\"A:after\"]");
            assertThat(MODEL.requests).hasSize(2);
            assertThat(MODEL.requests.get(1).path("messages").toString()).contains("12000", "tool_call_id");
            // 기본 메모리 Advisor는 도구 루프 밖: user/최종 assistant만 저장한다.
            assertThat(json(response).path("storedMessages").asInt()).isEqualTo(2);
        }
    }
    @Test void memoryIsScopedToSessionAndResetDoesNotAffectOtherSession() throws Exception {
        try (var alice = browser(); var bob = browser()) {
            post(alice, "/api/advisors/demo", "Alice의 질문");
            post(bob, "/api/advisors/demo", "Bob의 질문");
            assertThat(MODEL.requests.get(1).path("messages").toString()).doesNotContain("Alice");
            alice.send(HttpRequest.newBuilder(uri("/api/advisors/demo")).DELETE().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(json(post(alice, "/api/advisors/demo", "새 질문")).path("storedMessages").asInt()).isEqualTo(2);
            assertThat(json(post(bob, "/api/advisors/demo", "후속 질문")).path("storedMessages").asInt()).isEqualTo(4);
        }
    }
    HttpClient browser() { return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build(); }
    URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    HttpResponse<String> post(HttpClient http, String path, String text) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(text)).build(), HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> get(HttpClient http, String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonNode json(HttpResponse<String> response) { return LocalModelServer.JSON.readTree(response.body()); }
}
