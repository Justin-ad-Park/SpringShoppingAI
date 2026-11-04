package com.example.ailearning.testsupport;

import com.sun.net.httpserver.HttpExchange;
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

/** HTTP 수신·기록·전송을 담당한다. 제공 업체별 성공 응답은 등록한 핸들러가 만든다. */
final class StubHttpServer implements AutoCloseable {
    static final ObjectMapper JSON = new ObjectMapper();
    private record Request(JsonNode body, Map<String, String> headers) {}
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
        server.createContext("/", this::processRequestAndSendResponse);
        server.start();
    }

    private void processRequestAndSendResponse(HttpExchange exchange) {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            Request request = readJsonRequest(exchange);
            recorded.computeIfAbsent(path, ignored -> new CopyOnWriteArrayList<>()).add(request);
            // 응답을 먼저 확정한다. 지연 중 설정이 바뀌어도 현재 응답은 유지한다.
            Response response = createResponseForRequestPath(path, request.body());
            Thread.sleep(response.delayMs());
            sendJsonResponse(exchange, response);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
            // 타임아웃 테스트에서 클라이언트가 먼저 연결을 닫을 수 있다.
        }
    }

    private Request readJsonRequest(HttpExchange exchange) throws IOException {
        byte[] input = exchange.getRequestBody().readAllBytes();
        JsonNode body = input.length == 0 ? JSON.createObjectNode() : JSON.readTree(input);
        var headers = exchange.getRequestHeaders().entrySet().stream()
                .collect(Collectors.toMap(
                entry -> entry.getKey().toLowerCase(Locale.ROOT), entry -> entry.getValue().getFirst()));
        return new Request(body, Map.copyOf(headers));
    }

    private Response createResponseForRequestPath(String path, JsonNode body) {
        var handler = routes.get(path);
        if (handler == null) return new Response(404, createErrorResponseBody("unknown_route"), 0);
        try {
            return handler.apply(body);
        } catch (RuntimeException error) {
            // 응답 미설정·순서 소진 등 시나리오 실행 실패는 성공 응답으로 대체하지 않는다.
            return new Response(500, createErrorResponseBody("stub_response_not_configured"), 0);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, Response response) throws IOException {
        byte[] bytes = response.body().toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(response.status(), bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    /**
     * code에 "rate_limit_exceeded"를 전달한 오류 본문 예시. message와 code는 인자로 받은 오류 코드이다.
     * <pre>{@code
     * {
     *   "error": {
     *     "message": "rate_limit_exceeded",
     *     "type": "api_error",
     *     "code": "rate_limit_exceeded"
     *   }
     * }
     * }</pre>
     */
    static JsonNode createErrorResponseBody(String code) {
        var body = JSON.createObjectNode();
        body.putObject("error")
                .put("message", code)
                .put("type", "api_error")
                .put("code", code);
        return body;
    }

    void registerResponseHandler(String path, Function<JsonNode, Response> handler) {
        routes.put(path, handler);
    }

    String getBaseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    List<JsonNode> getRequestBodies(String path) {
        return recorded.getOrDefault(path, List.of()).stream()
                .map(Request::body)
                .toList();
    }

    List<String> getRequestHeaderValues(String path, String name) {
        String headerName = name.toLowerCase(Locale.ROOT);
        return recorded.getOrDefault(path, List.of()).stream()
                .map(request -> request.headers().get(headerName))
                .toList();
    }

    void clearRecordedRequests(String path) {
        recorded.remove(path);
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
