package com.example.shopping.tutor.lesson.l04memory.support;

import org.springframework.ai.chat.messages.*;

import java.util.*;

/** window 크기를 바꾸어 비교하기 위한 고정 4턴의 대화. */
public final class DemoConversationMessages {
    private DemoConversationMessages() {}

    public static List<Message> eightMessages() {
        List<Message> messages = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            if (index % 2 == 0) {
                messages.add(new UserMessage("질문 " + index));
            } else {
                messages.add(new AssistantMessage("답변 " + index));
            }
        }
        return messages;
    }
}
