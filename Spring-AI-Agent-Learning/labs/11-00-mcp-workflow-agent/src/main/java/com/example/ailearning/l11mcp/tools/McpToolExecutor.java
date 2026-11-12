package com.example.ailearning.l11mcp.tools;

import com.example.ailearning.l11mcp.domain.SearchConditions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static com.example.ailearning.l11mcp.McpRunResult.ToolCall;

/** 요청 한 건의 MCP 도구를 상한 안에서 실행하고, 조건 검사와 실행 결과 기록을 추가한다. */
public final class McpToolExecutor {
    private final ShoppingMcpGateway shoppingMcpGateway;
    private final ObjectMapper objectMapper;
    private final int maxToolCallAttempts;
    private final SearchConditions fixedSearchConditions;
    private final List<ToolCall> completedToolCalls = new ArrayList<>();
    private int toolCallAttempts;
    private List<String> searchedProductIds;
    private JsonNode lastCartQuote;

    public McpToolExecutor(ShoppingMcpGateway shoppingMcpGateway, ObjectMapper objectMapper,
            int maxToolCallAttempts, SearchConditions fixedSearchConditions) {
        this.shoppingMcpGateway = shoppingMcpGateway;
        this.objectMapper = objectMapper;
        this.maxToolCallAttempts = maxToolCallAttempts;
        this.fixedSearchConditions = fixedSearchConditions;
    }

    public JsonNode executeTool(String toolName, Map<String, Object> arguments) {
        String argumentsJson = objectMapper.writeValueAsString(arguments);
        String toolResponse = createRecordedToolCallback(toolName).call(argumentsJson);
        return shoppingMcpGateway.parseToolResult(toolResponse);
    }

    public ToolCallback[] createToolCallbacks() {
        return new ToolCallback[] {
                createRecordedToolCallback("searchProducts"),
                createRecordedToolCallback("quoteCart") };
    }

    private ToolCallback createRecordedToolCallback(String toolName) {
        if (!List.of("searchProducts", "quoteCart").contains(toolName)) {
            throw new IllegalArgumentException("이 요청에서 허용하지 않은 도구입니다.");
        }
        ToolCallback remoteToolCallback = shoppingMcpGateway.getToolCallback(toolName);
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                // MCP callback의 연결별 접두사 대신 이 실습의 원래 도구 이름을 사용한다.
                return ToolDefinition.builder()
                        .name(toolName)
                        .description(remoteToolCallback.getToolDefinition().description())
                        .inputSchema(remoteToolCallback.getToolDefinition().inputSchema())
                        .build();
            }

            @Override
            public String call(String argumentsJson) {
                return executeAndRecordToolCall(toolName, remoteToolCallback, argumentsJson);
            }
        };
    }

    private String executeAndRecordToolCall(String toolName, ToolCallback remoteToolCallback, String argumentsJson) {
        if (++toolCallAttempts > maxToolCallAttempts) {
            throw new IllegalStateException("MCP 도구 호출 상한을 초과했습니다.");
        }
        JsonNode toolArguments = objectMapper.readTree(argumentsJson);
        if (fixedSearchConditions != null) {
            toolArguments = buildRestrictedArguments(toolName, toolArguments);
        }
        String toolResponse = remoteToolCallback.call(objectMapper.writeValueAsString(toolArguments));
        JsonNode toolResult = shoppingMcpGateway.parseToolResult(toolResponse);
        recordToolResult(toolName, toolArguments, toolResult);
        return toolResponse;
    }

    private void recordToolResult(String toolName, JsonNode toolArguments, JsonNode toolResult) {
        completedToolCalls.add(new ToolCall(toolName, toolArguments, toolResult));
        if (toolName.equals("searchProducts")) {
            searchedProductIds = readProductIds(toolResult);
            lastCartQuote = null;
        } else {
            lastCartQuote = toolResult;
        }
    }

    /** 검색 조건은 Java가 고정하고, 계산 도구에는 검색한 전체 ID만 전달되게 한다. */
    private JsonNode buildRestrictedArguments(String toolName, JsonNode toolArguments) {
        if (toolName.equals("searchProducts")) {
            return objectMapper.valueToTree(fixedSearchConditions.searchArguments());
        }
        List<String> requestedProductIds = new ArrayList<>();
        for (JsonNode productId : toolArguments.path("additionalProductIds")) {
            requestedProductIds.add(productId.asString());
        }
        requireAllSearchedProductIds(requestedProductIds);
        // scenario는 사용자 인증 정보가 아니라 서버의 고정 데모 데이터 이름이다.
        return objectMapper.valueToTree(Map.of("scenario", "empty", "additionalProductIds", requestedProductIds));
    }

    private void requireAllSearchedProductIds(List<String> requestedProductIds) {
        if (searchedProductIds == null || requestedProductIds.size() != searchedProductIds.size()
                || !new HashSet<>(requestedProductIds).equals(new HashSet<>(searchedProductIds))) {
            throw new IllegalArgumentException("검색 결과 전체를 중복 없이 계산해야 합니다.");
        }
    }

    public static List<String> readProductIds(JsonNode products) {
        List<String> productIds = new ArrayList<>();
        for (JsonNode product : products) {
            productIds.add(product.path("id").asString());
        }
        return List.copyOf(productIds);
    }

    public List<ToolCall> toolCalls() {
        return List.copyOf(completedToolCalls);
    }

    public boolean hasEmptyProductSearchResult() {
        return searchedProductIds != null && searchedProductIds.isEmpty();
    }

    public JsonNode getLastCartQuote() {
        return lastCartQuote;
    }
}
