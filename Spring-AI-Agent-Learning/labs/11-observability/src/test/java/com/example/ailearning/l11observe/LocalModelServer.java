package com.example.ailearning.l11observe;

import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 제공된 로컬 HTTP 검증 도구. 실제 모델의 판단을 흉내 내지 않습니다. */
final class LocalModelServer implements AutoCloseable {
    static final ObjectMapper JSON = new ObjectMapper();
    final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    volatile String answer = "YES";
    private final HttpServer server;

    LocalModelServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v1/chat/completions", exchange -> {
                try (exchange) {
                    requests.add(JSON.readTree(exchange.getRequestBody()));
                    var response = JSON.createObjectNode().put("id", "local").put("object", "chat.completion")
                            .put("created", 1).put("model", "learning-model");
                    response.putObject("usage").put("prompt_tokens", 10).put("completion_tokens", 5).put("total_tokens", 15);
                    var choice = response.putArray("choices").addObject().put("index", 0).put("finish_reason", "stop");
                    choice.putObject("message").put("role", "assistant").put("content", answer);
                    byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                }
            });
            server.start();
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
    String url() { return "http://127.0.0.1:"+server.getAddress().getPort()+"/v1"; }
    public void close() { server.stop(0); }
}
