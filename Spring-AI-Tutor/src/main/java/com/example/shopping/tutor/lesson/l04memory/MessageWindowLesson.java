package com.example.shopping.tutor.lesson.l04memory;

import com.example.shopping.tutor.lesson.l04memory.support.DemoConversationMessages;

import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.*;

import java.util.List;

/** 메시지 개수 제한이다. 긴 메시지의 토큰 비용까지 제한하지는 않는다. */
public final class MessageWindowLesson {
    public List<Message> keepRecentMessages(int maxMessages) {
        var memory = MessageWindowChatMemory.builder().maxMessages(maxMessages).build();
        for (Message message : DemoConversationMessages.eightMessages()) {
            memory.add("demo", message);
        }
        return memory.get("demo");
    }
}
