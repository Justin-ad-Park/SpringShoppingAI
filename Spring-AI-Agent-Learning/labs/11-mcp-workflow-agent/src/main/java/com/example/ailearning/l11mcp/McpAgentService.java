package com.example.ailearning.l11mcp;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class McpAgentService {
    private static final String INSTRUCTION = """
            TASK:MCP_AGENT
            제공된 MCP 도구 중 필요한 도구와 인자를 선택하세요.
            상품 가격 상한에 맞는 MALL 배송그룹의 재고 있는 상품을 검색하세요.
            모두 구매한다면 검색된 상품 전체를 각 1개씩 계산하세요.
            quoteCart의 scenario는 empty로 지정합니다. 실제 장바구니는 변경하지 않습니다.
            상품 가격, 배송비, 총액은 도구 결과로 확인하고 총예산이 있으면 비교하세요.
            도구가 필요 없는 인사에는 도구 없이 답하세요.
            """;
    private final ChatClient chatClient;
    private final ShoppingMcpGateway shoppingMcpGateway;
    private final ObjectMapper objectMapper;
    private final int maxToolCalls;

    public McpAgentService(ChatClient.Builder builder,
            ShoppingMcpGateway shoppingMcpGateway, ObjectMapper objectMapper,
            @Value("${learning.mcp.max-tool-calls}") int maxToolCalls) {
        this.chatClient = builder.build();
        this.shoppingMcpGateway = shoppingMcpGateway;
        this.objectMapper = objectMapper;
        this.maxToolCalls = maxToolCalls;
    }

    public McpRunResult agent(String question) {
        McpToolRun toolRun = new McpToolRun(shoppingMcpGateway, objectMapper, maxToolCalls, null);
        String answer = askWithMcpTools(question, toolRun);
        return new McpRunResult("ANSWERED", answer, toolRun.toolCalls());
    }

    String askWithMcpTools(String question, McpToolRun toolRun) {
        return chatClient
                .prompt()
                .system(INSTRUCTION)
                .user(question)
                .toolCallbacks(toolRun.callbacks())
                .call()
                .content();
    }
}
