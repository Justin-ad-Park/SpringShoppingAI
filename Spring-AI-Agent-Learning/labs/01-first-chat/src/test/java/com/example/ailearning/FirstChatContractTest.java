package com.example.ailearning;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 Spring AI 어댑터를 사용하되 외부 모델 API만 로컬 HTTP 서버로 대체합니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FirstChatContractTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<JsonNode> REQUESTS = new CopyOnWriteArrayList<>();
    private static final List<String> AUTH_HEADERS = new CopyOnWriteArrayList<>();
    private static final HttpServer MODEL_SERVER = startModelServer();

    @Value("${local.server.port}")
    private int port;

    @DynamicPropertySource
    static void modelProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "tutor-test-model");
        registry.add("spring.ai.openai.base-url",
                () -> "http://127.0.0.1:" + MODEL_SERVER.getAddress().getPort() + "/v1");
    }

    @BeforeEach
    void resetRequests() {
        REQUESTS.clear();
        AUTH_HEADERS.clear();
    }

    @AfterAll
    static void stopModelServer() {
        MODEL_SERVER.stop(0);
    }

    @Test
    void sendsUserMessageThroughSpringAiAndReturnsText() throws Exception {
        HttpResponse<String> response = ask("스프링이 무엇인가요?");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("로컬 테스트 모델의 고정 응답입니다.");
        assertThat(REQUESTS).hasSize(1);
        JsonNode request = REQUESTS.getFirst();
        assertThat(request.path("model").asText()).isEqualTo("tutor-test-model");
        assertThat(request.path("messages").size()).isEqualTo(1);
        assertThat(request.at("/messages/0/role").asText()).isEqualTo("user");
        assertThat(request.at("/messages/0/content").asText()).isEqualTo("스프링이 무엇인가요?");
        assertThat(AUTH_HEADERS).containsExactly("Bearer local-test-key");
    }

    @Test
    void rejectsBlankAndOversizedInputsBeforeCallingModel() throws Exception {
        assertThat(ask("   ").statusCode()).isEqualTo(400);
        assertThat(ask("가".repeat(2001)).statusCode()).isEqualTo(400);
        assertThat(REQUESTS).isEmpty();
    }

    @Test
    void doesNotAutomaticallyRememberPreviousRequest() throws Exception {
        assertThat(ask("제 이름은 민수입니다.").statusCode()).isEqualTo(200);
        assertThat(ask("제 이름이 무엇인가요?").statusCode()).isEqualTo(200);

        assertThat(REQUESTS).hasSize(2);
        JsonNode second = REQUESTS.get(1);
        assertThat(second.path("messages").size()).isEqualTo(1);
        assertThat(second.at("/messages/0/content").asText()).isEqualTo("제 이름이 무엇인가요?");
    }

    private HttpResponse<String> ask(String message) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/chat"))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(message))
                .build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private static HttpServer startModelServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v1/chat/completions", exchange -> {
                REQUESTS.add(JSON.readTree(exchange.getRequestBody()));
                AUTH_HEADERS.add(exchange.getRequestHeaders().getFirst("Authorization"));
                byte[] response = """
                        {"id":"chatcmpl-local","object":"chat.completion","created":0,
                         "model":"tutor-test-model","choices":[{"index":0,
                         "message":{"role":"assistant","content":"로컬 테스트 모델의 고정 응답입니다."},
                         "finish_reason":"stop"}],
                         "usage":{"prompt_tokens":10,"completion_tokens":10,"total_tokens":20}}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                try (var body = exchange.getResponseBody()) {
                    body.write(response);
                }
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("로컬 테스트 모델 서버를 시작하지 못했습니다.", e);
        }
    }
}
