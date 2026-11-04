package com.example.ailearning.l07memory;

import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.OpenAiStubServer;
import static com.example.ailearning.testsupport.OpenAiStubServer.text;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import java.net.*;
import java.net.http.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class MemoryContractTest {
    private TestOutput output;

    @BeforeEach
    void 테스트_이름_준비(TestInfo info) {
        output = new TestOutput(info.getTestMethod().orElseThrow().getName());
    }

    static final OpenAiStubServer MODEL = new OpenAiStubServer();
    @Value("${local.server.port}") int port;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::baseUrl);
    }
    @BeforeEach void reset() {
        MODEL.resetChat();
        MODEL.respondWith(text("고정 응답"));
    }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void 이전_질문과_답변을_새_질문_앞에_전달한다() throws Exception {
        try (var alice = browser()) {
            post(alice, "/api/memory/demo", "예산은 3만원입니다.");
            var response = post(alice, "/api/memory/demo", "음료로 추천해줘.");
            assertThat(response.statusCode()).isEqualTo(200);
            JsonNode messages = MODEL.chatRequests().get(1).path("messages");
            assertThat(messages.size()).isEqualTo(4);
            assertThat(messages.get(0).path("role").asString()).isEqualTo("system");
            assertThat(messages.get(1).path("content").asString()).isEqualTo("예산은 3만원입니다.");
            assertThat(messages.get(2).path("role").asString()).isEqualTo("assistant");
            assertThat(messages.get(3).path("content").asString()).isEqualTo("음료로 추천해줘.");
            assertThat(json(response).path("storedMessages").asInt()).isEqualTo(4);
        }
    }
    @Test void 메모리_없는_요청은_이전_대화를_전달하지_않는다() throws Exception {
        try (var alice = browser()) {
            post(alice, "/api/stateless", "제 이름은 민수입니다.");
            post(alice, "/api/stateless", "제 이름은?");
            assertThat(MODEL.chatRequests().get(1).path("messages").size()).isEqualTo(2);
            assertThat(MODEL.chatRequests().get(1).path("messages").toString()).doesNotContain("민수");
        }
    }
    @Test void 대화_ID가_같아도_사용자_세션이_다르면_분리된다() throws Exception {
        try (var alice = browser(); var bob = browser()) {
            post(alice, "/api/memory/demo", "Alice의 예산은 3만원");
            post(bob, "/api/memory/demo", "Bob의 예산은 1만원");
            assertThat(MODEL.chatRequests().get(1).path("messages").toString()).doesNotContain("Alice");
            assertThat(get(bob, "/api/memory/demo").body()).doesNotContain("Alice");
            assertThat(get(alice, "/api/memory/demo").body()).contains("Alice").doesNotContain("Bob");
        }
    }
    @Test void 대화_삭제는_해당_사용자의_지정한_대화에만_적용된다() throws Exception {
        try (var alice = browser(); var bob = browser()) {
            post(alice, "/api/memory/one", "Alice one");
            post(alice, "/api/memory/two", "Alice two");
            post(bob, "/api/memory/one", "Bob one");
            output.response(alice.send(HttpRequest.newBuilder(uri("/api/memory/one")).DELETE().build(), HttpResponse.BodyHandlers.ofString()));
            assertThat(json(get(alice, "/api/memory/one")).size()).isZero();
            assertThat(get(alice, "/api/memory/two").body()).contains("Alice two");
            assertThat(get(bob, "/api/memory/one").body()).contains("Bob one");
        }
    }
    @Test void 최근_메시지_여섯_개를_보관하고_전달할_메시지_수를_제한한다() throws Exception {
        try (var alice = browser()) {
            for (int turn = 1; turn <= 5; turn++) post(alice, "/api/memory/window", "질문" + turn);
            assertThat(json(get(alice, "/api/memory/window")).size()).isEqualTo(6);
            JsonNode sent = MODEL.chatRequests().get(4).path("messages");
            assertThat(sent.size()).isEqualTo(8);
            assertThat(sent.toString()).doesNotContain("질문1").contains("질문2", "질문5");
        }
    }
    @Test void 실패하거나_빈_응답이면_이번_대화를_저장하지_않는다() throws Exception {
        try (var alice = browser()) {
            post(alice, "/api/memory/failure", "성공한 질문");
            MODEL.respondWith(text(""));
            assertThat(post(alice, "/api/memory/failure", "실패한 질문").statusCode()).isEqualTo(502);
            var history = get(alice, "/api/memory/failure");
            assertThat(json(history).size()).isEqualTo(2);
            assertThat(history.body()).doesNotContain("실패한 질문");
        }
    }
    @Test void 잘못된_입력은_모델을_호출하지_않는다() throws Exception {
        try (var alice = browser()) {
            assertThat(post(alice, "/api/memory/demo", " ").statusCode()).isEqualTo(400);
            assertThat(post(alice, "/api/memory/invalid%3Aid", "질문").statusCode()).isEqualTo(400);
            assertThat(MODEL.chatRequests()).isEmpty();
        }
    }
    HttpClient browser() { return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build(); }
    URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    HttpResponse<String> post(HttpClient http, String path, String text) throws Exception {
        return output.response(http.send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(text)).build(), HttpResponse.BodyHandlers.ofString()));
    }
    HttpResponse<String> get(HttpClient http, String path) throws Exception {
        return output.response(http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString()));
    }
    JsonNode json(HttpResponse<String> response) { return OpenAiStubServer.JSON.readTree(response.body()); }
}
