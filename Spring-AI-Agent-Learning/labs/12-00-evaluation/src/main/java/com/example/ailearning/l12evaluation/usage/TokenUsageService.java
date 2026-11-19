package com.example.ailearning.l12evaluation.usage;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** 12-3: 채팅 응답 한 번에서 답변과 토큰 수를 함께 읽는다. */
@Service
public class TokenUsageService {
    public record Reply(String answer, Integer inputTokens, Integer outputTokens) {}
    private final ChatClient chatClient;

    public TokenUsageService(ChatClient.Builder builder) {
        chatClient = builder.build();
    }

    public Reply ask(String question) {
        var response = chatClient
                .prompt()
                .user(question)
                .call()
                .chatResponse();
        var usage = response.getMetadata().getUsage();
        boolean reported = usage != null && usage.getTotalTokens() > 0;
        return new Reply(response.getResult().getOutput().getText(),
                reported ? usage.getPromptTokens() : null,
                reported ? usage.getCompletionTokens() : null);
    }
}
