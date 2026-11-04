package com.example.ailearning.testsupport;

import tools.jackson.databind.JsonNode;
import java.util.List;

/** Anthropic의 메시지 응답 형식을 재현하는 테스트 도구. */
public final class AnthropicStubServer implements AutoCloseable {
    private final StubHttpServer http = new StubHttpServer();
    private volatile String answer;

    public AnthropicStubServer() {
        http.route("/v1/messages", request -> {
            String content = answer;
            if (content == null) throw new IllegalStateException("응답을 지정하세요.");
            var body = StubHttpServer.JSON.createObjectNode().put("id", "msg-local").put("type", "message")
                    .put("role", "assistant").put("model", request.path("model").asString())
                    .put("stop_reason", "end_turn").putNull("stop_sequence");
            body.putArray("content").addObject().put("type", "text").put("text", content);
            body.putObject("usage").put("input_tokens", 10).put("output_tokens", 5);
            return new StubHttpServer.Response(200, body, 0);
        });
    }
    public void respondText(String content) { answer = content; }
    public void reset() { http.clear("/v1/messages"); answer = null; }
    public String baseUrl() { return http.baseUrl(); }
    public List<JsonNode> requests() { return http.requests("/v1/messages"); }
    public List<String> apiKeyHeaders() { return http.headers("/v1/messages", "x-api-key"); }
    public void close() { http.close(); }
}
