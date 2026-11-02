package com.example.shopping.integration.support;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.example.shopping.integration.support.ChatClient.JSON;
import static org.assertj.core.api.Assertions.assertThat;

/** 실제 모델 대신 고정 응답을 반환한다. 모델 품질이 아니라 API·MCP 연결 경로를 검사한다. */
public final class OpenAiStub implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ConcurrentLinkedQueue<String> calls = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

    OpenAiStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.setExecutor(executor);
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public long count(String kind) {
        return calls.stream().filter(kind::equals).count();
    }

    public void assertHealthy() {
        // HTTP 스레드에서 난 검증 실패도 JUnit 결과에 반드시 반영한다.
        assertThat(failures).as("GPT 대역 내부의 검증 실패").isEmpty();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                assertThat(exchange.getRequestMethod()).isEqualTo("POST");
                JsonNode request = JSON.readTree(exchange.getRequestBody());
                String user = userMessages(request);
                ObjectNode message = createMessage(request, user);
                send(exchange, 200, completion(message));
            } catch (Exception | AssertionError error) {
                failures.add(error);
                send(exchange, 500, JSON.createObjectNode().put("error", error.toString()));
            }
        }
    }

    private static String userMessages(JsonNode request) {
        return StreamSupport.stream(request.path("messages").spliterator(), false)
                .filter(message -> message.path("role").asText().equals("user"))
                .map(message -> message.path("content").asText())
                .collect(Collectors.joining("\n"));
    }

    private ObjectNode createMessage(JsonNode request, String user) throws Exception {
        // API 프로토콜의 요청 종류 구분이다. 테스트 실행 모드 선택과는 별개다.
        if (request.path("tools").isEmpty()) {
            calls.add("interpret");
            return JSON.createObjectNode().put("role", "assistant")
                    .put("content", interpret(user).toString());
        }
        return catalogMessage(request, user);
    }

    private ObjectNode interpret(String user) throws Exception {
        String text = user.split("현재 발화: ", 2)[1];
        var decision = (ObjectNode) JSON.readTree("""
                {"action":"NEW", "capabilityId":"cart.freeShippingRecommendation",
                 "keyword":null, "category":null, "budget":null,
                 "coupons":false, "points":false, "question":null}
                """);

        if (text.contains("음료")) {
            // 이어지는 발화에 기존 목적과 예산이 함께 전달되어야 한다.
            assertThat(user).contains("cart.freeShippingRecommendation", "\"maxPrice\":30000");
            decision.put("action", "REFINE");
            decision.set("category", change("음료"));
            return decision;
        }

        decision.set("budget", JSON.createObjectNode().put("operation", "SET").put("value", 30000));
        if (text.contains("텀블러")) {
            decision.put("capabilityId", "catalog.search");
            decision.set("keyword", change("텀블러"));
        }
        return decision;
    }

    private static ObjectNode change(String value) {
        return JSON.createObjectNode().put("operation", "SET").put("value", value);
    }

    private ObjectNode catalogMessage(JsonNode request, String user) throws Exception {
        List<String> names = StreamSupport.stream(request.path("tools").spliterator(), false)
                .map(tool -> tool.path("function").path("name").asText()).toList();
        assertThat(names).containsExactly("searchProducts");

        boolean hasToolResult = StreamSupport.stream(request.path("messages").spliterator(), false)
                .anyMatch(message -> message.path("role").asText().equals("tool"));
        var message = JSON.createObjectNode().put("role", "assistant");
        if (hasToolResult) {
            calls.add("catalog-selection");
            String id = user.contains("잘못된 선택") ? "invented" : "P009";
            var selection = JSON.createObjectNode();
            selection.putArray("productIds").add(id);
            return message.put("content", selection.toString());
        }

        // 첫 응답은 도구 실행 요청이다. 다음 요청에서 MCP 실행 결과를 받는다.
        calls.add("catalog-tool");
        message.putNull("content");
        var call = message.putArray("tool_calls").addObject()
                .put("id", "call-catalog").put("type", "function");
        call.putObject("function").put("name", "searchProducts").put("arguments", """
                {"keyword":"텀블러", "minPrice":0, "maxPrice":30000, "category":null,
                 "excludeProductIds":[], "shippingGroups":[], "inStockOnly":true,
                 "sort":"PRICE_ASC", "limit":20}
                """);
        return message;
    }

    private static ObjectNode completion(ObjectNode message) {
        var payload = JSON.createObjectNode().put("id", "chatcmpl-stub")
                .put("object", "chat.completion").put("created", 1).put("model", "gpt-4.1-mini");
        var choice = payload.putArray("choices").addObject().put("index", 0)
                .put("finish_reason", message.has("tool_calls") ? "tool_calls" : "stop");
        choice.set("message", message);
        payload.putObject("usage").put("prompt_tokens", 10)
                .put("completion_tokens", 10).put("total_tokens", 20);
        return payload;
    }

    private static void send(HttpExchange exchange, int status, JsonNode payload) throws IOException {
        byte[] data = payload.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, data.length);
        exchange.getResponseBody().write(data);
    }

    @Override
    public void close() {
        server.stop(0);
        executor.close();
    }
}
