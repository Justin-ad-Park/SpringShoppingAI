package com.example.ailearning.l09search;

import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 키워드별 직교 벡터를 반환하는 테스트 전용 API. 실제 의미 이해가 아니다. */
final class EmbeddingWireServer implements AutoCloseable {
    static final ObjectMapper JSON = new ObjectMapper();
    final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private final HttpServer server;
    EmbeddingWireServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/embeddings", exchange -> {
            try (exchange) {
                JsonNode request = JSON.readTree(exchange.getRequestBody()); requests.add(request);
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
        });server.start();
    }
    String url() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1"; }
    public void close() { server.stop(0); }
}
