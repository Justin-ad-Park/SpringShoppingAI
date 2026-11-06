package com.example.ailearning.l05tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ToolChatService {
    private final ChatClient client;
    private final ShoppingTools tools;

    public ToolChatService(ChatClient.Builder builder, ShoppingTools tools) {
        this.client = builder.build();
        this.tools = tools;
    }

    public String ask(String question) {
        return client
                .prompt()
                .system("""
                        가상 상품 상담을 합니다. 가격과 배송비는 도구 결과만 사용하세요.
                        필요에 따라 상품 검색과 배송비 조회 도구를 선택하세요.
                        배송비를 물으면 상품 가격과 구분해서 설명하세요.
                        상품 조회가 필요 없는 요청에는 도구 없이 답할 수 있습니다.
                        """)
                .user(question)
                .tools(tools)
                .call()
                .content();
    }
}
