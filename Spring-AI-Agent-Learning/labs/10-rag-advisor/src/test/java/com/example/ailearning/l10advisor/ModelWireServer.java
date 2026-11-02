package com.example.ailearning.l10advisor;

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
final class ModelWireServer implements AutoCloseable {
    static final ObjectMapper JSON = new ObjectMapper();
    final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    final List<JsonNode> embeddingRequests = new CopyOnWriteArrayList<>();
    volatile int chatStatus = 200;
    volatile long chatDelayMs;
    volatile Function<JsonNode, ObjectNode> responder = request -> message("고정 응답");
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final HttpServer server;

    ModelWireServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/v1/chat/completions", exchange -> {
            try (exchange) {
                JsonNode request = JSON.readTree(exchange.getRequestBody());
                requests.add(request);
                if (chatDelayMs > 0) {
                    try { Thread.sleep(chatDelayMs); }
                    catch (InterruptedException error) { Thread.currentThread().interrupt(); return; }
                }
                if (chatStatus != 200) {
                    byte[] failure = "{\"error\":{\"message\":\"local rate limit\",\"type\":\"rate_limit_error\",\"code\":\"rate_limit_exceeded\"}}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(chatStatus, failure.length);
                    exchange.getResponseBody().write(failure);
                    return;
                }
                ObjectNode response = JSON.createObjectNode().put("id", "local")
                        .put("object", "chat.completion").put("created", 1).put("model", "learning-model");
                response.putObject("usage").put("prompt_tokens", 10).put("completion_tokens", 5).put("total_tokens", 15);
                var choice = response.putArray("choices").addObject().put("index", 0);
                choice.set("message", responder.apply(request));
                choice.put("finish_reason", "stop");
                byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.createContext("/v1/embeddings", exchange -> {
            try (exchange) {
                JsonNode request = JSON.readTree(exchange.getRequestBody()); embeddingRequests.add(request);
                var root = JSON.createObjectNode().put("object", "list").put("model", "learning-embedding");
                root.putObject("usage").put("prompt_tokens", 3).put("total_tokens", 3);
                var data = root.putArray("data");
                JsonNode input = request.path("input");
                List<String> texts = input.isArray() ? java.util.stream.StreamSupport.stream(input.spliterator(), false).map(JsonNode::asString).toList() : List.of(input.asString());
                for (int i = 0; i < texts.size(); i++) {
                    String text = texts.get(i);
                    var vector = data.addObject().put("object", "embedding").put("index", i).putArray("embedding");
                    if (text.contains("배송") || text.contains("운송")) vector.add(1.0).add(0.0).add(0.0);
                    else if (text.contains("반품") || text.contains("개봉")) vector.add(0.0).add(1.0).add(0.0);
                    else vector.add(0.0).add(0.0).add(1.0);
                }
                byte[] bytes = root.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
    }

    static ObjectNode message(String content) {
        return JSON.createObjectNode().put("role", "assistant").put("content", content);
    }

    String url() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1"; }

    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
