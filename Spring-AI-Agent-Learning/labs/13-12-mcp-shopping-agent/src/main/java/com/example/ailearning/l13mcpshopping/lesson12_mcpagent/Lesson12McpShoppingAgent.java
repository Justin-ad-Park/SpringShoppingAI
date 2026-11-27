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
            이전 대화와 이번 질문에서 필요한 값을 확인하세요.
            상품 추천의 필수 정보는 예산이고, 반품 기간 안내에 필요한 정보는 개봉 여부입니다.
            질문에 '미개봉 반품'이라고 적혀 있거나 고객이 개봉하지 않았다고 답하면 개봉 상태를 SEALED로 판단하세요.
            필요한 값이 빠졌으면 조회를 시작하기 전에 빠진 값만 모두 질문하고 고객의 답을 기다리세요.
            상품 추천에는 searchProducts로 MALL 배송그룹의 후보를 찾으세요. 예산은 별도 언급이 없으면 배송비 포함입니다.
            추천 후보의 총액은 반드시 quoteCart에 scenario=empty와 해당 상품 ID 하나를 넘겨 조회하세요.
            견적이 유효하고 총액이 예산 이내인 상품을 추천하세요. 답변에는 조회한 배송비와 총액을 명시하세요.
            반품 질문만 있으면 상품 조회는 필요 없습니다. 개봉 상태에 맞는 getReturnPolicy 결과로 반품 기준을 안내하세요.
            반품 안내에는 기준일과 기간 또는 조건을 적고, 도구 결과의 sourceId 값을 출처로 그대로 적으세요.
            필요한 근거를 확인하지 못했다면 한계를 알려주세요.
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
