package com.example.ailearning.l08advisors.lesson05_order;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** 08-5: 등록한 나열 순서와 getOrder가 정하는 진입 순서를 구분한다. */
@Service
public class Lesson05AdvisorOrderChatService {
    private final ChatClient chatClient;

    public Lesson05AdvisorOrderChatService(ChatClient.Builder builder) {
        chatClient = builder
                // B를 먼저 나열해도 order가 작은 A가 먼저 실행된다.
                .defaultAdvisors(new OrderedLogAdvisor("B", 10), new OrderedLogAdvisor("A", 0))
                .build();
    }

    public String chat(String question) {
        return chatClient
                .prompt()
                .user(question)
                .call()
                .content();
    }
}
