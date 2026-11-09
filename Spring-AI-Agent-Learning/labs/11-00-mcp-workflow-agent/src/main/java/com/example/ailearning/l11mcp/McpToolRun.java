package com.example.ailearning.l11mcp;

import com.example.ailearning.l11mcp.domain.SearchConditions;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static com.example.ailearning.l11mcp.McpRunResult.ToolCall;

/** 요청별 도구 기록·호출 상한·하이브리드 조건을 관리하는 보조 코드. 11-5에서 읽는다. */
public final class McpToolRun {
    private final ShoppingMcpGateway shoppingMcpGateway;
    private final ObjectMapper objectMapper;
    private final int maxToolCalls;
    private final SearchConditions fixedConditions;
    private final List<ToolCall> toolCalls = new ArrayList<>();
    private int attemptedCalls;
    private List<String> searchedProductIds;
    private JsonNode lastQuote;

    public McpToolRun(ShoppingMcpGateway shoppingMcpGateway, ObjectMapper objectMapper,
            int maxToolCalls, SearchConditions fixedConditions) {
        this.shoppingMcpGateway = shoppingMcpGateway;
        this.objectMapper = objectMapper;
        this.maxToolCalls = maxToolCalls;
        this.fixedConditions = fixedConditions;
    }

    public JsonNode call(String name, Map<String, Object> arguments) {
        return shoppingMcpGateway.readJsonToolResult(createRecordedToolCallback(name).call(objectMapper.writeValueAsString(arguments)));
    }

    public ToolCallback[] callbacks() {
        return new ToolCallback[] { createRecordedToolCallback("searchProducts"), createRecordedToolCallback("quoteCart") };
    }

    private ToolCallback createRecordedToolCallback(String name) {
        if (!List.of("searchProducts", "quoteCart").contains(name)) {
            throw new IllegalArgumentException("이 요청에서 허용하지 않은 도구입니다.");
        }
        ToolCallback remoteTool = shoppingMcpGateway.getToolCallback(name);
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                // MCP callback의 연결별 접두사 대신 이 실습의 원래 도구 이름을 사용한다.
                return ToolDefinition
                        .builder()
                        .name(name)
                        .description(remoteTool.getToolDefinition().description())
                        .inputSchema(remoteTool.getToolDefinition().inputSchema())
                        .build();
            }

            @Override
            public String call(String input) {
                if (++attemptedCalls > maxToolCalls) {
                    throw new IllegalStateException("MCP 도구 호출 상한을 초과했습니다.");
                }
                JsonNode arguments = objectMapper.readTree(input);
                if (fixedConditions != null) {
                    arguments = restrictArguments(name, arguments);
                }
                String response = remoteTool.call(objectMapper.writeValueAsString(arguments));
                JsonNode result = shoppingMcpGateway.readJsonToolResult(response);
                toolCalls.add(new ToolCall(name, arguments, result));
                if (name.equals("searchProducts")) {
                    searchedProductIds = readProductIds(result);
                    lastQuote = null;
                } else {
                    lastQuote = result;
                }
                return response;
            }
        };
    }

    /** 검색 조건은 Java가 고정하고, 모델은 검색 결과의 전체 ID를 계산 도구에 전달해야 한다. */
    private JsonNode restrictArguments(String name, JsonNode arguments) {
        if (name.equals("searchProducts")) {
            return objectMapper.valueToTree(fixedConditions.searchArguments());
        }
        List<String> requestedIds = new ArrayList<>();
        for (JsonNode id : arguments.path("additionalProductIds")) {
            requestedIds.add(id.asString());
        }
        if (searchedProductIds == null || requestedIds.size() != searchedProductIds.size()
                || !new HashSet<>(requestedIds).equals(new HashSet<>(searchedProductIds))) {
            throw new IllegalArgumentException("검색 결과 전체를 중복 없이 계산해야 합니다.");
        }
        // scenario는 사용자 인증 정보가 아니라 서버의 고정 데모 데이터 이름이다.
        return objectMapper.valueToTree(Map.of("scenario", "empty", "additionalProductIds", requestedIds));
    }

    public static List<String> readProductIds(JsonNode products) {
        List<String> ids = new ArrayList<>();
        for (JsonNode product : products) {
            ids.add(product.path("id").asString());
        }
        return List.copyOf(ids);
    }

    public List<ToolCall> toolCalls() {
        return List.copyOf(toolCalls);
    }

    public boolean searchedWithoutResults() {
        return searchedProductIds != null && searchedProductIds.isEmpty();
    }

    public JsonNode lastQuote() {
        return lastQuote;
    }
}
