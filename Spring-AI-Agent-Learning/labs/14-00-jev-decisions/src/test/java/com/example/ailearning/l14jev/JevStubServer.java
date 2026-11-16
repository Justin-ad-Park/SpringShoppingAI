package com.example.ailearning.l14jev;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Jev 전용 고정 응답 HTTP 대역입니다. 의미 판단 대신 요청 형식·역직렬화·분기를 검증합니다. */
final class JevStubServer implements AutoCloseable {
    static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpServer server;
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private volatile String responseBody = "{}";
    private volatile int status = 200;
    private volatile String authorization;

    JevStubServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException error) {
            throw new IllegalStateException("Jev 테스트 서버를 시작하지 못했습니다.", error);
        }
        server.createContext("/v1/systemone", exchange -> {
            try (exchange) {
                authorization = exchange.getRequestHeaders().getFirst("Authorization");
                requests.add(JSON.readTree(exchange.getRequestBody().readAllBytes()));
                byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, response.length);
                exchange.getResponseBody().write(response);
            }
        });
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    void reset() {
        requests.clear();
        authorization = null;
        respond(200, "{}");
    }

    void respond(int status, String body) {
        this.status = status;
        this.responseBody = body;
    }

    List<JsonNode> requests() {
        return List.copyOf(requests);
    }

    String authorization() {
        return authorization;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
