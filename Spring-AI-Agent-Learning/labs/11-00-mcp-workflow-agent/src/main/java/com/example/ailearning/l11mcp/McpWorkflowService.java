package com.example.ailearning.l11mcp;

import com.example.ailearning.l11mcp.tools.McpToolExecutor;
import com.example.ailearning.l11mcp.tools.ShoppingMcpGateway;

import com.example.ailearning.l11mcp.domain.SearchConditions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

@Service
public class McpWorkflowService {
    private final SearchConditionExtractor searchConditionExtractor;
    private final ShoppingMcpGateway shoppingMcpGateway;
    private final ObjectMapper objectMapper;
    private final int maxToolCallAttempts;

    public McpWorkflowService(SearchConditionExtractor searchConditionExtractor,
            ShoppingMcpGateway shoppingMcpGateway, ObjectMapper objectMapper,
            @Value("${learning.mcp.max-tool-calls}") int maxToolCallAttempts) {
        this.searchConditionExtractor = searchConditionExtractor;
        this.shoppingMcpGateway = shoppingMcpGateway;
        this.objectMapper = objectMapper;
        this.maxToolCallAttempts = maxToolCallAttempts;
    }

    public McpRunResult run(String question) {
        SearchConditions searchConditions = searchConditionExtractor.extract(question);
        if (!searchConditions.isComplete()) {
            return new McpRunResult("NEEDS_INPUT", "카테고리와 상품 가격 상한을 알려주세요.", List.of());
        }
        McpToolExecutor toolExecutor = new McpToolExecutor(shoppingMcpGateway, objectMapper, maxToolCallAttempts, null);
        JsonNode products = toolExecutor.executeTool("searchProducts", searchConditions.searchArguments());
        List<String> productIds = McpToolExecutor.readProductIds(products);
        if (productIds.isEmpty()) {
            return new McpRunResult("NO_RESULTS", "조건에 맞는 상품이 없습니다.", toolExecutor.toolCalls());
        }
        JsonNode cartQuote = toolExecutor.executeTool("quoteCart",
                Map.of("scenario", "empty", "additionalProductIds", productIds));
        if (!cartQuote.path("valid").asBoolean()) {
            throw new IllegalStateException("장바구니 금액을 계산할 수 없습니다.");
        }
        boolean overBudget = searchConditions.totalBudget() != null
                && cartQuote.path("total").asLong() > searchConditions.totalBudget();
        return new McpRunResult(overBudget ? "OVER_BUDGET" : "COMPLETED",
                "상품 ID: " + productIds + ", 배송비 포함 총액: " + cartQuote.path("total").asLong() + "원",
                toolExecutor.toolCalls());
    }
}
