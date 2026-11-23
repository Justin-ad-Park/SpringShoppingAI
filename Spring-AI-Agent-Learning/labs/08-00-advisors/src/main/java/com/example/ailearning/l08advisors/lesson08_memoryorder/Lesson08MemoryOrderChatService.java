package com.example.ailearning.l08advisors.lesson08_memoryorder;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;

/** 08-8: 테스트에서 준비한 메모리 Advisor로 도구를 사용하는 대화를 실행한다. */
public class Lesson08MemoryOrderChatService {
    private final ChatClient chatClient;
    private final TeaPriceTools teaPriceTools;

    public Lesson08MemoryOrderChatService(ChatModel chatModel,
            MessageChatMemoryAdvisor memoryAdvisor, TeaPriceTools teaPriceTools) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("상품 가격은 반드시 제공된 도구로 확인한 뒤 답하세요.")
                .defaultAdvisors(memoryAdvisor)
                .build();
        this.teaPriceTools = teaPriceTools;
    }

    public String chat(String conversationId, String question) {
        return chatClient
                .prompt()
                .user(question)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(teaPriceTools)
                .call()
                .content();
    }
}
