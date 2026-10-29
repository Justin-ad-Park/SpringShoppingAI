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

    /** 같은 ID를 전달하면 대화가 이어진다. 웹의 사용자 격리는 platform이 담당한다. */
    public String chatWithMemory(String conversationId, String question) {
        MessageChatMemoryAdvisor memoryAdvisor =
                MessageChatMemoryAdvisor.builder(chatMemory).build();
        // Advisor가 이번 요청에 이전 메시지를 붙이고, 성공한 응답을 기억한다.
        return chatClient
                .prompt()
                .advisors(memoryAdvisor)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(question)
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
