package com.example.ailearning.l11mcp;

import com.example.ailearning.l11mcp.tools.McpToolExecutor;
import com.example.ailearning.l11mcp.tools.ShoppingMcpGateway;

import com.example.ailearning.l11mcp.domain.SearchConditions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.List;

@Service
public class McpHybridService {
    private final SearchConditionExtractor searchConditionExtractor;
    private final McpAgentService agentService;
    private final ShoppingMcpGateway shoppingMcpGateway;
    private final ObjectMapper objectMapper;
    private final int maxToolCallAttempts;

    public McpHybridService(SearchConditionExtractor searchConditionExtractor, McpAgentService agentService,
            ShoppingMcpGateway shoppingMcpGateway, ObjectMapper objectMapper,
            @Value("${learning.mcp.max-tool-calls}") int maxToolCallAttempts) {
        this.searchConditionExtractor = searchConditionExtractor;
        this.agentService = agentService;
        this.shoppingMcpGateway = shoppingMcpGateway;
        this.objectMapper = objectMapper;
        this.maxToolCallAttempts = maxToolCallAttempts;
    }

    public McpRunResult run(String question) {
        SearchConditions searchConditions = searchConditionExtractor.extract(question);
        if (!searchConditions.isComplete()) {
            return new McpRunResult("NEEDS_INPUT", "카테고리와 상품 가격 상한을 알려주세요.", List.of());
        }
        McpToolExecutor toolExecutor = new McpToolExecutor(shoppingMcpGateway, objectMapper, maxToolCallAttempts, searchConditions);
        agentService.requestAnswerWithTools(question, toolExecutor);
        if (toolExecutor.hasEmptyProductSearchResult()) {
            return new McpRunResult("NO_RESULTS", "조건에 맞는 상품이 없습니다.", toolExecutor.toolCalls());
        }
        JsonNode cartQuote = toolExecutor.getLastCartQuote();
        if (cartQuote == null || !cartQuote.path("valid").asBoolean()) {
            return new McpRunResult("UNVERIFIED", "전체 상품의 금액 계산을 확인하지 못했습니다.", toolExecutor.toolCalls());
        }
        long cartTotal = cartQuote.path("total").asLong();
        if (searchConditions.totalBudget() != null && cartTotal > searchConditions.totalBudget()) {
            return new McpRunResult("OVER_BUDGET", "배송비 포함 " + cartTotal + "원으로 총예산을 초과합니다.", toolExecutor.toolCalls());
        }
        // 모델의 최종 문장 대신 실제 MCP 결과로 표시할 금액을 확정한다.
        return new McpRunResult("VERIFIED", "배송비 포함 총액: " + cartTotal + "원", toolExecutor.toolCalls());
    }

}
