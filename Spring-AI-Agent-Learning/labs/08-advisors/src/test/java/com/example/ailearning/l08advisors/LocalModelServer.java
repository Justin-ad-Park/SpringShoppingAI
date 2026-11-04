package com.example.ailearning.l08advisors;

import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/** 로컬 고정 응답 서버. 실제 모델 성능을 흉내 내지 않는다. */
final class LocalModelServer implements AutoCloseable {
    static final ObjectMapper JSON = new ObjectMapper();
    final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    volatile int statusCode = 200;
    volatile Function<JsonNode, ObjectNode> responder = request -> message("고정 응답");
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final HttpServer server;

    LocalModelServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/v1/chat/completions", exchange -> {
            try (exchange) {
                JsonNode request = JSON.readTree(exchange.getRequestBody());
                requests.add(request);
                ObjectNode response = JSON.createObjectNode().put("id", "local")
                        .put("object", "chat.completion").put("created", 1).put("model", "learning-model");
                var choice = response.putArray("choices").addObject().put("index", 0);
                choice.set("message", responder.apply(request));
                choice.put("finish_reason", "stop");
                byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(statusCode, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
    }

    static ObjectNode message(String content) {
        return JSON.createObjectNode().put("role", "assistant").put("content", content);
    }

    static ObjectNode tool(String id, String name, String arguments) {
        var message = JSON.createObjectNode().put("role", "assistant").putNull("content");
        var call = message.putArray("tool_calls").addObject().put("id", id).put("type", "function");
        call.putObject("function").put("name", name).put("arguments", arguments);
        return message;
    }

    String url() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1"; }

    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
