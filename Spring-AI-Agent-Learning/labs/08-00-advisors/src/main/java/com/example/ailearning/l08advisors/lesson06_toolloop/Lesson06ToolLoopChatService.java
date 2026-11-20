package com.example.ailearning.l08advisors.lesson06_toolloop;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** 08-6: 사용자 정의 Advisor 없이 도구 실행과 후속 모델 요청만 확인한다. */
@Service
public class Lesson06ToolLoopChatService {
    private final ChatClient chatClient;
    private final PriceTools priceTools;

    public Lesson06ToolLoopChatService(ChatClient.Builder builder, PriceTools priceTools) {
        chatClient = builder
                .defaultSystem("상품 가격은 반드시 제공된 도구로 확인한 값으로 안내하세요.")
                .build();
        this.priceTools = priceTools;
    }

    public String chat(String question) {
        return chatClient
                .prompt()
                .user(question)
                .tools(priceTools)
                .call()
                .content();
    }
}
