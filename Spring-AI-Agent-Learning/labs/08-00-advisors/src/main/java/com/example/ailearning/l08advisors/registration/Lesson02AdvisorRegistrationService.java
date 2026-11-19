package com.example.ailearning.l08advisors.registration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.stereotype.Service;

/** 08-2: 기본 등록과 이번 요청에만 추가하는 등록의 범위만 비교한다. */
@Service
public class Lesson02AdvisorRegistrationService {
    private final ChatClient chatClient;

    public Lesson02AdvisorRegistrationService(ChatClient.Builder builder) {
        chatClient = builder
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
                .build();
    }

    public String chatWithDefaults(String question) {
        return chatClient
                .prompt()
                .user(question)
                .call()
                .content();
    }

    public String chatWithAdditionalAdvisor(String question, Advisor additionalAdvisor) {
        return chatClient
                .prompt()
                .user(question)
                // 기본 Advisor를 교체하지 않는다. 이번 요청에만 추가한다.
                .advisors(additionalAdvisor)
                .call()
                .content();
    }
}
