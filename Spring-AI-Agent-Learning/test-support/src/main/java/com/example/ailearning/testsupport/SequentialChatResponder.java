package com.example.ailearning.testsupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/** 준비된 assistant 메시지를 순서대로 반환하며, 모두 사용하면 실패한다. */
final class SequentialChatResponder implements Function<JsonNode, ObjectNode> {
    private final List<ObjectNode> messagesInResponseOrder;
    private final AtomicInteger nextMessageIndex = new AtomicInteger();

    SequentialChatResponder(ObjectNode... assistantMessages) {
        messagesInResponseOrder = Arrays.stream(assistantMessages)
                .map(ObjectNode::deepCopy)
                .toList();
    }

    /** Function이 정한 메서드 이름이며, 호출할 때마다 다음 assistant 메시지를 반환한다. */
    @Override
    public ObjectNode apply(JsonNode request) {
        int responseIndex = nextMessageIndex.getAndIncrement();
        if (responseIndex >= messagesInResponseOrder.size()) {
            throw new IllegalStateException("준비한 응답을 모두 사용했습니다.");
        }
        return messagesInResponseOrder.get(responseIndex);
    }
}
