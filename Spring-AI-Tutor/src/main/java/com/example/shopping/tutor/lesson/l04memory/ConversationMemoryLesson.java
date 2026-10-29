package com.example.shopping.tutor.lesson.l04memory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;

/** 04: 같은 대화 ID로 이전 질문과 답변을 모델 요청에 다시 포함한다. */
public final class ConversationMemoryLesson {
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ConversationMemoryLesson(ChatModel chatModel, ChatMemory chatMemory) {
        this.chatClient = ChatClient.create(chatModel);
        this.chatMemory = chatMemory;
    }

    /** 같은 conversationId를 쓰면 앞 대화를 이번 질문과 함께 보낸다. */
    public String chatWithMemory(String conversationId, String question) {
        MessageChatMemoryAdvisor memoryAdvisor =
                MessageChatMemoryAdvisor.builder(chatMemory).build();
        // 이전 대화를 이번 요청에 붙인다. 대화 이름이 다르면 다른 기록을 사용한다.
        return chatClient
                .prompt()
                .advisors(memoryAdvisor)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(question) // 이번에 입력한 질문이다. 이전 대화는 위에서 함께 넣는다.
                .call()
                .content();
    }

    public record Comparison(String withoutMemory, String withMemory) {}

    /** 두 요청의 프롬프트를 관찰한다. 이전 대화는 두 번째 요청에만 들어간다. */
    public Comparison compareNoMemoryAndMemory(String conversationId, String question) {
        String without = chatClient.prompt().user(question).call().content();
        String with = chatWithMemory(conversationId, question);
        return new Comparison(without, with);
    }
}
