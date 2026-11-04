package com.example.ailearning.testsupport;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;

/** Anthropic의 메시지 응답 형식을 재현하는 테스트 도구. */
public final class AnthropicStubServer implements AutoCloseable {
    private static final String MESSAGES = "/v1/messages";
    private final StubHttpServer http = new StubHttpServer();
    private volatile String answer; //쓰레드 원자성 보장

    public AnthropicStubServer() {
        http.registerResponseHandler(MESSAGES, this::createMessageHttpResponse);
    }

    /**
     * 응답 본문 예시. model은 요청의 값, content의 text는 setMessageResponseText로 지정한 값이다. usage는 테스트용 고정값이다.
     * <pre>{@code
     * {
     *   "id": "msg-local",
     *   "type": "message",
     *   "role": "assistant",
     *   "model": "claude-example",
     *   "stop_reason": "end_turn",
     *   "stop_sequence": null,
     *   "content": [
     *     {
     *       "type": "text",
     *       "text": "안녕하세요!"
     *     }
     *   ],
     *   "usage": {
     *     "input_tokens": 10,
     *     "output_tokens": 5
     *   }
     * }
     * }</pre>
     */
    private StubHttpServer.Response createMessageHttpResponse(JsonNode request) {
        String content = answer;
        if (content == null) throw new IllegalStateException("응답을 지정하세요.");

        var body = StubHttpServer.JSON.createObjectNode()
                .put("id", "msg-local")
                .put("type", "message")
                .put("role", "assistant")
                .put("model", request.path("model").asString())
                .put("stop_reason", "end_turn")
                .putNull("stop_sequence");

        body.putArray("content")
                .addObject()
                .put("type", "text")
                .put("text", content);

        body.putObject("usage")
                .put("input_tokens", 10)
                .put("output_tokens", 5);

        return new StubHttpServer.Response(200, body, 0);
    }

    public void setMessageResponseText(String content) {
        answer = Objects.requireNonNull(content);
    }

    public void resetMessageResponseAndRequests() {
        http.clearRecordedRequests(MESSAGES);
        answer = null;
    }

    public String getBaseUrl() {
        return http.getBaseUrl();
    }

    public List<JsonNode> getMessageRequests() {
        return http.getRequestBodies(MESSAGES);
    }

    public List<String> getMessageApiKeyHeaders() {
        return http.getRequestHeaderValues(MESSAGES, "x-api-key");
    }

    @Override
    public void close() {
        http.close();
    }
}
