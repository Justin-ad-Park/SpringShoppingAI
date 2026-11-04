package com.example.ailearning.testsupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

/** 채팅 응답 설정의 변경·초기화와 요청별 설정 스냅샷을 담당한다. */
final class ChatResponseSettings {
    record Snapshot(Function<JsonNode, ObjectNode> assistantMessageForRequest,
                    int httpStatus, long responseDelayMs,
                    OpenAiResponseBody.TokenUsage tokenUsage) {}

    private Function<JsonNode, ObjectNode> assistantMessageForRequest = createMissingChatResponseHandler();
    private int httpStatus = 200;
    private long responseDelayMs;
    private OpenAiResponseBody.TokenUsage tokenUsage;

    // 변경 중간의 상태를 읽지 않도록 설정 변경과 스냅샷 생성에 같은 잠금을 사용한다.
    synchronized Snapshot createSnapshot() {
        return new Snapshot(assistantMessageForRequest, httpStatus, responseDelayMs, tokenUsage);
    }

    synchronized void setAssistantMessageFunction(Function<JsonNode, ObjectNode> assistantMessageForRequest) {
        this.assistantMessageForRequest = Objects.requireNonNull(assistantMessageForRequest);
    }

    synchronized void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    synchronized void setResponseDelay(Duration responseDelay) {
        if (responseDelay.isNegative()) {
            throw new IllegalArgumentException("지연은 0 이상이어야 합니다.");
        }
        responseDelayMs = responseDelay.toMillis();
    }

    synchronized void setTokenUsage(int inputTokens, int outputTokens) {
        tokenUsage = new OpenAiResponseBody.TokenUsage(inputTokens, outputTokens);
    }

    /** 사용량 설정은 유지하고 응답·HTTP 상태·지연을 초기화한다. */
    synchronized void resetResponseKeepingTokenUsage() {
        assistantMessageForRequest = createMissingChatResponseHandler();
        httpStatus = 200;
        responseDelayMs = 0;
    }

    private static Function<JsonNode, ObjectNode> createMissingChatResponseHandler() {
        return request -> {
            throw new IllegalStateException("응답 시나리오를 지정하세요.");
        };
    }
}
