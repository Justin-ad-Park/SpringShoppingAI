package com.example.ailearning.l11mcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.List;

@Service
public class McpHybridService {
    private final SearchConditionExtractor conditionExtractor;
    private final McpAgentService agentService;
    private final ShoppingMcpGateway shoppingMcpGateway;
    private final ObjectMapper objectMapper;
    private final int maxToolCalls;

    public McpHybridService(SearchConditionExtractor conditionExtractor, McpAgentService agentService,
            ShoppingMcpGateway shoppingMcpGateway, ObjectMapper objectMapper,
            @Value("${learning.mcp.max-tool-calls}") int maxToolCalls) {
        this.conditionExtractor = conditionExtractor;
        this.agentService = agentService;
        this.shoppingMcpGateway = shoppingMcpGateway;
        this.objectMapper = objectMapper;
        this.maxToolCalls = maxToolCalls;
    }

    public McpRunResult run(String question) {
        SearchConditions conditions = conditionExtractor.extract(question);
        if (!conditions.isComplete()) {
            return new McpRunResult("NEEDS_INPUT", "카테고리와 상품 가격 상한을 알려주세요.", List.of());
        }
        McpToolRun toolRun = new McpToolRun(shoppingMcpGateway, objectMapper, maxToolCalls, conditions);
        agentService.askWithMcpTools(question, toolRun);
        JsonNode quote = toolRun.lastQuote();
        if (quote == null || !quote.path("valid").asBoolean()) {
            return new McpRunResult("UNVERIFIED", "전체 상품의 금액 계산을 확인하지 못했습니다.", toolRun.toolCalls());
        }
        long total = quote.path("total").asLong();
        if (conditions.totalBudget() != null && total > conditions.totalBudget()) {
            return new McpRunResult("OVER_BUDGET", "배송비 포함 " + total + "원으로 총예산을 초과합니다.", toolRun.toolCalls());
        }
        // 모델의 최종 문장 대신 실제 MCP 결과로 표시할 금액을 확정한다.
        return new McpRunResult("VERIFIED", "배송비 포함 총액: " + total + "원", toolRun.toolCalls());
    }

}
