package com.example.ailearning.l13mcpshopping.lesson12_mcpagent;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

/** 고객 대화와 MCP 서버에서 발견한 도구를 모델에 전달한다. */
@Service
public class Lesson12McpShoppingAgent {
    private static final String MCP_SHOPPING_AGENT_INSTRUCTIONS = """
            쇼핑 상담원입니다. 한 메시지에 요청이 여러 개라면 각각 답하세요.
            상품 추천에는 searchProducts를 사용하세요. 견적을 낼 후보는 MALL 배송그룹에서 찾으세요.
            예산은 별도 언급이 없으면 배송비 포함입니다.
            추천 후보 하나의 배송비와 총액은 quoteCart에 scenario=empty와 해당 상품 ID 하나를 넘겨 확인하세요.
            반품 질문에는 상품의 개봉 상태에 맞는 getReturnPolicy를 호출하고 결과의 출처 ID를 함께 알려주세요.
            예산이나 개봉 여부처럼 고객만 알려줄 수 있는 필수 정보가 빠졌으면 해당 항목을 물으세요.
            앞선 대화에서 확인한 정보는 다음 고객 메시지와 함께 참고하세요.
            상품 가격, 총액, 반품 조건은 도구 결과를 근거로 답하고, 확인하지 못한 항목은 그렇게 알려주세요.
            """;

    private final ChatClient chatClient;
    private final ToolCallbackProvider mcpToolProvider;

    public Lesson12McpShoppingAgent(ChatClient.Builder chatClientBuilder, ToolCallbackProvider mcpToolProvider) {
        this.chatClient = chatClientBuilder.build();
        this.mcpToolProvider = mcpToolProvider;
    }

    public String answer(String customerQuestion, List<Message> previousMessages) {
        return chatClient
                .prompt()
                .system(MCP_SHOPPING_AGENT_INSTRUCTIONS)
                .messages(previousMessages)
                .user(customerQuestion)
                .tools(mcpToolProvider)
                .call()
                .content();
    }
}
