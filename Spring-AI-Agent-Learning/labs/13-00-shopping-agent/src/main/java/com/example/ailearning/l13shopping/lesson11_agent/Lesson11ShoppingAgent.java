package com.example.ailearning.l13shopping.lesson11_agent;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

/** 고객의 대화와 사용 가능한 도구를 모델에 전달한다. */
@Service
public class Lesson11ShoppingAgent {
    private static final String SHOPPING_AGENT_INSTRUCTIONS = """
            쇼핑 상담원입니다. 한 메시지에서 요청한 일이 여러 개라면 각각 답하세요.
            상품 추천에는 상품 검색과 배송비 포함 총액 조회를 활용하세요. 예산은 별도 언급이 없으면 배송비 포함입니다.
            반품 기간과 조건은 상품의 개봉 상태에 맞는 정책을 조회하고, 해당 출처를 함께 알려주세요.
            예산이나 개봉 상태처럼 고객만 알려줄 수 있는 필수 정보가 빠졌으면 해당 항목을 물으세요.
            앞선 대화에서 확인한 정보는 다음 고객 메시지와 함께 참고하세요.
            상품 가격, 배송비, 반품 조건은 도구 결과를 근거로 답하고, 확인하지 못한 항목은 그렇게 알려주세요.
            """;

    private final ChatClient chatClient;
    private final Lesson11ShoppingTools shoppingTools;

    public Lesson11ShoppingAgent(ChatClient.Builder chatClientBuilder, Lesson11ShoppingTools shoppingTools) {
        this.chatClient = chatClientBuilder.build();
        this.shoppingTools = shoppingTools;
    }

    public String answer(String customerQuestion, List<Message> previousMessages) {
        return chatClient
                .prompt()
                .system(SHOPPING_AGENT_INSTRUCTIONS)
                .messages(previousMessages)
                .user(customerQuestion)
                .tools(shoppingTools)
                .call()
                .content();
    }
}
