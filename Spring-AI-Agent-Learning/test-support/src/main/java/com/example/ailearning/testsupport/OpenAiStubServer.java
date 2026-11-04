package com.example.ailearning.testsupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/** OpenAI HTTP 응답 형식만 재현한다. 실제 모델 판단과 벡터 생성은 하지 않는다. */
public final class OpenAiStubServer implements AutoCloseable {
    public static final ObjectMapper JSON = StubHttpServer.JSON;
    private static final String CHAT = "/v1/chat/completions", EMBEDDING = "/v1/embeddings";
    private final StubHttpServer http = new StubHttpServer();
    private volatile Function<JsonNode, ObjectNode> responder = unconfigured();
    private volatile Function<String, float[]> embedding = text -> { throw new IllegalStateException("임베딩 값을 지정하세요."); };
    private volatile int status = 200;
    private volatile long delayMs;
    private volatile int[] usage;

    public OpenAiStubServer() {
        http.route(CHAT, request -> {
            int code = status;
            long wait = delayMs;
            if (code != 200) return new StubHttpServer.Response(code, StubHttpServer.error(
                    code == 429 ? "rate_limit_exceeded" : "stub_failure"), wait);
            ObjectNode message = responder.apply(request).deepCopy();
            ObjectNode body = JSON.createObjectNode().put("id", "local").put("object", "chat.completion")
                    .put("created", 1).put("model", request.path("model").asString());
            int[] tokens = usage;
            if (tokens != null) body.putObject("usage").put("prompt_tokens", tokens[0])
                    .put("completion_tokens", tokens[1]).put("total_tokens", tokens[0] + tokens[1]);
            var choice = body.putArray("choices").addObject().put("index", 0);
            choice.set("message", message);
            choice.put("finish_reason", message.has("tool_calls") ? "tool_calls" : "stop");
            return new StubHttpServer.Response(200, body, wait);
        });
        http.route(EMBEDDING, request -> {
            var body = JSON.createObjectNode().put("object", "list").put("model", request.path("model").asString());
            body.putObject("usage").put("prompt_tokens", 3).put("total_tokens", 3);
            var data = body.putArray("data");
            JsonNode input = request.path("input");
            List<String> texts = input.isArray() ? StreamSupport.stream(input.spliterator(), false).map(JsonNode::asString).toList() : List.of(input.asString());
            Function<String, float[]> configured = embedding;
            for (int index = 0; index < texts.size(); index++) {
                var vector = data.addObject().put("object", "embedding").put("index", index).putArray("embedding");
                for (float value : configured.apply(texts.get(index))) vector.add(value);
            }
            return new StubHttpServer.Response(200, body, 0);
        });
    }

    public static ObjectNode text(String content) { return JSON.createObjectNode().put("role", "assistant").put("content", content); }
    public static ObjectNode toolCall(String id, String name, String arguments) {
        var message = JSON.createObjectNode().put("role", "assistant").putNull("content");
        var call = message.putArray("tool_calls").addObject().put("id", id).put("type", "function");
        call.putObject("function").put("name", name).put("arguments", arguments);
        return message;
    }
    private static Function<JsonNode, ObjectNode> unconfigured() {
        return request -> { throw new IllegalStateException("응답 시나리오를 지정하세요."); };
    }
    public OpenAiStubServer respondWith(ObjectNode response) {
        ObjectNode snapshot = response.deepCopy();
        return respondUsing(request -> snapshot);
    }
    public OpenAiStubServer respondUsing(Function<JsonNode, ObjectNode> response) { responder = response; return this; }
    public OpenAiStubServer respondInOrder(ObjectNode... responses) {
        List<ObjectNode> snapshots = java.util.Arrays.stream(responses).map(ObjectNode::deepCopy).toList();
        AtomicInteger index = new AtomicInteger();
        return respondUsing(request -> snapshots.get(index.getAndIncrement()));
    }
    public OpenAiStubServer embeddingUsing(Function<String, float[]> values) { embedding = values; return this; }
    public OpenAiStubServer status(int code) { status = code; return this; }
    public OpenAiStubServer delay(Duration duration) {
        if (duration.isNegative()) throw new IllegalArgumentException("지연은 0 이상이어야 합니다.");
        delayMs = duration.toMillis(); return this;
    }
    public OpenAiStubServer usage(int input, int output) { usage = new int[]{input, output}; return this; }
    /** 채팅 기록·응답·오류·지연만 초기화한다. 임베딩 적재 기록·정의와 usage 설정은 유지한다. */
    public void resetChat() { http.clear(CHAT); responder = unconfigured(); status = 200; delayMs = 0; }
    public String baseUrl() { return http.baseUrl() + "/v1"; }
    public List<JsonNode> chatRequests() { return http.requests(CHAT); }
    public List<JsonNode> embeddingRequests() { return http.requests(EMBEDDING); }
    public List<String> authorizationHeaders() { return http.headers(CHAT, "Authorization"); }
    public void close() { http.close(); }
}
