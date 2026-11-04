package com.example.ailearning.testsupport;

import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 테스트 전용 HTTP 처리. 제공 업체와 단원의 응답 내용은 알지 못한다. */
final class StubHttpServer implements AutoCloseable {
    static final ObjectMapper JSON = new ObjectMapper();
    record Request(JsonNode body, Map<String, String> headers) {}
    record Response(int status, JsonNode body, long delayMs) {}
    private final Map<String, Function<JsonNode, Response>> routes = new ConcurrentHashMap<>();
    private final Map<String, List<Request>> recorded = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final HttpServer server;

    StubHttpServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException error) {
            executor.shutdownNow();
            throw new IllegalStateException("테스트 서버를 시작하지 못했습니다.", error);
        }
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            try (exchange) {
                String path = exchange.getRequestURI().getPath();
                byte[] input = exchange.getRequestBody().readAllBytes();
                JsonNode body = input.length == 0 ? JSON.createObjectNode() : JSON.readTree(input);
                var headers = exchange.getRequestHeaders().entrySet().stream().collect(Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(Locale.ROOT), entry -> entry.getValue().getFirst()));
                recorded.computeIfAbsent(path, ignored -> new CopyOnWriteArrayList<>()).add(new Request(body, headers));
                Response response;
                try {
                    var route = routes.get(path);
                    response = route == null ? new Response(404, error("unknown_route"), 0) : route.apply(body);
                } catch (RuntimeException error) {
                    response = new Response(500, error("stub_response_not_configured"), 0);
                }
                // 응답을 먼저 확정한다. 지연 중 다음 테스트가 설정을 바꿔도 현재 응답은 유지한다.
                if (response.delayMs() > 0) {
                    try { Thread.sleep(response.delayMs()); }
                    catch (InterruptedException error) { Thread.currentThread().interrupt(); return; }
                }
                byte[] bytes = response.body().toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(response.status(), bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (IOException ignored) {
                // 타임아웃 테스트에서 클라이언트가 먼저 연결을 닫을 수 있다.
            }
        });
        server.start();
    }

    static JsonNode error(String code) {
        var body = JSON.createObjectNode();
        body.putObject("error").put("message", code).put("type", "api_error").put("code", code);
        return body;
    }
    void route(String path, Function<JsonNode, Response> handler) { routes.put(path, handler); }
    String baseUrl() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    List<JsonNode> requests(String path) { return recorded.getOrDefault(path, List.of()).stream().map(Request::body).toList(); }
    List<String> headers(String path, String name) {
        return recorded.getOrDefault(path, List.of()).stream().map(request -> request.headers().get(name.toLowerCase(Locale.ROOT))).toList();
    }
    void clear(String path) { recorded.remove(path); }
    public void close() { server.stop(0); executor.shutdownNow(); }
}
