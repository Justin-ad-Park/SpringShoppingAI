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
            이전 대화와 이번 질문에서 필요한 값을 확인하세요.
            상품 추천의 필수 정보는 예산이고, 반품 기간 안내에 필요한 정보는 개봉 여부입니다.
            필요한 값이 빠졌으면 조회를 시작하기 전에 빠진 값만 모두 질문하고 고객의 답을 기다리세요.
            상품 추천은 검색한 후보의 배송비 포함 총액을 도구로 조회한 뒤, 그 총액을 예산과 비교해 결정하세요.
            예산은 별도 언급이 없으면 배송비 포함입니다. 답변에 추천 상품과 조회한 총액을 명시하세요.
            반품 정책을 조회한 뒤 개봉 상태에 맞는 문서의 기준일과 기간 또는 조건을 답하고, 출처 경로(location)도 그대로 적으세요.
            필요한 근거를 확인하지 못했다면 한계를 알려주세요.
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
