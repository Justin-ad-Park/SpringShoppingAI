package com.example.ailearning.testsupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.List;

/** OpenAI 응답의 JSON 구조만 만든다. 서버 실행과 시나리오 설정은 담당하지 않는다. */
final class OpenAiResponseBody {
    record TokenUsage(int input, int output) {}

    private OpenAiResponseBody() {}

    /**
     * assistant 메시지 예시. content는 인자로 받은 문장이다. 전체 HTTP 본문은 createChatResponseBody에서 만든다.
     * <pre>{@code
     * {
     *   "role": "assistant",
     *   "content": "안녕하세요!"
     * }
     * }</pre>
     */
    static ObjectNode createAssistantTextMessage(String content) {
        return StubHttpServer.JSON
                .createObjectNode()
                .put("role", "assistant")
                .put("content", content);
    }

    /**
     * 도구 호출 메시지 예시. id·name·arguments는 인자로 받은 값이다. arguments는 JSON 객체가 아닌 JSON 문자열이다.
     * <pre>{@code
     * {
     *   "role": "assistant",
     *   "content": null,
     *   "tool_calls": [
     *     {
     *       "id": "call-1",
     *       "type": "function",
     *       "function": {
     *         "name": "searchProducts",
     *         "arguments": "{\"category\":\"음료\"}"
     *       }
     *     }
     *   ]
     * }
     * }</pre>
     */
    static ObjectNode createAssistantToolCallMessage(String id, String name, String arguments) {
        var message = StubHttpServer.JSON
                .createObjectNode()
                .put("role", "assistant")
                .putNull("content");
        var call = message
                .putArray("tool_calls")
                .addObject()
                .put("id", id)
                .put("type", "function");
        call
                .putObject("function")
                .put("name", name)
                .put("arguments", arguments);
        return message;
    }

    /**
     * 채팅 응답 본문의 구조 예시. {$...}와 or 표기는 값의 위치·선택지를 나타내며 JSON 문법이 아니다.
     * model은 요청에서 읽고, usage가 null이면 usage 필드는 생략한다.
     * 도구 호출 메시지를 전달하면 message에 tool_calls가 들어가고 finish_reason도 "tool_calls"가 된다.
     * <pre>{@code
     * {
     *   "id": "local",
     *   "object": "chat.completion",
     *   "created": 1,
     *   "model": {$model},
     *   "usage": {
     *     "prompt_tokens": {$usage.input()},
     *     "completion_tokens": {$usage.output()},
     *     "total_tokens": {$usage.input() + $usage.output()}
     *   },
     *   "choices": [
     *     {
     *       "index": 0,
     *       "message": {       //파라미터 주입된 메시지
     *         "role": "assistant",
     *         "content": "안녕하세요!"
     *       },
     *       "finish_reason": "tool_calls" or "stop"
     *     }
     *   ]
     * }
     * }</pre>
     */
    static ObjectNode createChatResponseBody(JsonNode request, ObjectNode message, TokenUsage usage) {
        var body = StubHttpServer.JSON
                .createObjectNode()
                .put("id", "local")
                .put("object", "chat.completion")
                .put("created", 1)
                .put("model", request
                        .path("model")
                        .asString());
        if (usage != null) {
            body
                    .putObject("usage")
                    .put("prompt_tokens", usage.input())
                    .put("completion_tokens", usage.output())
                    .put("total_tokens", usage.input() + usage.output());
        }
        var choice = body
                .putArray("choices")
                .addObject()
                .put("index", 0);
        choice.set("message", message.deepCopy());
        choice.put("finish_reason", message.has("tool_calls") ? "tool_calls" : "stop");
        return body;
    }

    /**
     * 벡터 두 개를 전달한 HTTP 본문 예시. model은 요청에서 읽고, data는 전달한 벡터 순서로 만든다. usage는 테스트용 고정값이다.
     * <pre>{@code
     * {
     *   "object": "list",
     *   "model": "test-embedding-model",
     *   "usage": {
     *     "prompt_tokens": 3,
     *     "total_tokens": 3
     *   },
     *   "data": [
     *     {
     *       "object": "embedding",
     *       "index": 0,
     *       "embedding": [
     *         1.0,
     *         0.0
     *       ]
     *     },
     *     {
     *       "object": "embedding",
     *       "index": 1,
     *       "embedding": [
     *         0.0,
     *         1.0
     *       ]
     *     }
     *   ]
     * }
     * }</pre>
     */
    static ObjectNode createEmbeddingResponseBody(JsonNode request, List<float[]> vectors) {
        var body = StubHttpServer.JSON
                .createObjectNode()
                .put("object", "list")
                .put("model", request
                        .path("model")
                        .asString());
        body
                .putObject("usage")
                .put("prompt_tokens", 3)
                .put("total_tokens", 3);
        var data = body.putArray("data");
        for (int index = 0; index < vectors.size(); index++) {
            var vector = data
                    .addObject()
                    .put("object", "embedding")
                    .put("index", index)
                    .putArray("embedding");
            for (float value : vectors.get(index)) {
                vector.add(value);
            }
        }
        return body;
    }
}
