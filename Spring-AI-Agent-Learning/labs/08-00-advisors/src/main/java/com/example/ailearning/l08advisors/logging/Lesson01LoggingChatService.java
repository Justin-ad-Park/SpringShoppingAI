package com.example.ailearning.l08advisors.logging;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Service;

/** 08-1: 기본 제공 Advisor 하나를 연결해 실제 모델 요청·응답 로그를 확인한다. */
@Service
public class Lesson01LoggingChatService {
    private final ChatClient chatClient;

    public Lesson01LoggingChatService(ChatClient.Builder builder) {
        chatClient = builder
                // 이 클라이언트로 만드는 모든 요청에 같은 기록 기능을 적용한다.
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
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
