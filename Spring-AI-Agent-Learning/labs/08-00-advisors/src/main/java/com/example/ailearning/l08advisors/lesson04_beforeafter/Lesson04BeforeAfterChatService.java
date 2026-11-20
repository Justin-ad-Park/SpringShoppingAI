package com.example.ailearning.l08advisors.lesson04_beforeafter;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** 08-4: Advisor 하나의 전후 처리만 사용한다. */
@Service
public class Lesson04BeforeAfterChatService {
    private final ChatClient chatClient;

    public Lesson04BeforeAfterChatService(ChatClient.Builder builder) {
        chatClient = builder
                .defaultAdvisors(new BeforeAfterAdvisor())
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
