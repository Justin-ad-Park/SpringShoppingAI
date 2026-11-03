package com.example.ailearning.l02multiple;

import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** OpenAI와 Anthropic의 서로 다른 HTTP 형식만 재현하는 제공된 테스트 도구입니다. */
final class LocalProvidersServer implements AutoCloseable {
    static final ObjectMapper JSON = new ObjectMapper();
    final List<JsonNode> openAiRequests = new CopyOnWriteArrayList<>();
    final List<JsonNode> anthropicRequests = new CopyOnWriteArrayList<>();
    final List<String> openAiAuth = new CopyOnWriteArrayList<>();
    final List<String> anthropicAuth = new CopyOnWriteArrayList<>();
    private final HttpServer server;

    LocalProvidersServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v1/chat/completions", exchange -> {
                try (exchange) {
                    openAiRequests.add(JSON.readTree(exchange.getRequestBody()));
                    openAiAuth.add(exchange.getRequestHeaders().getFirst("Authorization"));
                    String json = """
                            {"id":"chat-local","object":"chat.completion","created":1,"model":"openai-test-model",
                             "choices":[{"index":0,"message":{"role":"assistant","content":"OpenAI 로컬 고정 응답"},"finish_reason":"stop"}],
                             "usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}
                            """;
                    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes);
                }
            });
            server.createContext("/v1/messages", exchange -> {
                try (exchange) {
                    anthropicRequests.add(JSON.readTree(exchange.getRequestBody()));
                    anthropicAuth.add(exchange.getRequestHeaders().getFirst("x-api-key"));
                    String json = """
                            {"id":"msg-local","type":"message","role":"assistant","model":"anthropic-test-model",
                             "content":[{"type":"text","text":"Anthropic 로컬 고정 응답"}],"stop_reason":"end_turn","stop_sequence":null,
                             "usage":{"input_tokens":10,"output_tokens":5}}
                            """;
                    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes);
                }
            });
            server.start();
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
    String baseUrl() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    public void close() { server.stop(0); }
}
