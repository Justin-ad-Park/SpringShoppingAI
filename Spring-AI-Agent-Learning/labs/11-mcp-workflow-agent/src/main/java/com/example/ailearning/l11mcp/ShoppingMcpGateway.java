package com.example.ailearning.l11mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** MCP 연결과 결과 형식 처리를 담당한다. listToolNames와 call부터 읽는다. */
@Component
public class ShoppingMcpGateway {
    private final Map<String, ToolCallback> callbacks = new LinkedHashMap<>();
    private final ObjectMapper objectMapper;

    public ShoppingMcpGateway(List<McpSyncClient> mcpClients, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        if (mcpClients.size() != 1) {
            throw new IllegalStateException("이 실습은 shopping MCP 서버 하나에 연결합니다.");
        }
        McpSyncClient mcpClient = mcpClients.getFirst();
        // 서버에서 얻은 도구 설명과 입력 스키마를 모델에도 사용한다.
        for (var tool : mcpClient.listTools().tools()) {
            callbacks.put(tool.name(), new SyncMcpToolCallback(mcpClient, tool));
        }
    }

    public List<String> listToolNames() {
        return List.copyOf(callbacks.keySet());
    }

    public ToolCallback getToolCallback(String toolName) {
        ToolCallback callback = callbacks.get(toolName);
        if (callback == null) {
            throw new IllegalArgumentException("등록되지 않은 MCP 도구: " + toolName);
        }
        return callback;
    }

    public JsonNode call(String toolName, Map<String, Object> arguments) {
        String response = getToolCallback(toolName)
                .call(objectMapper.writeValueAsString(arguments));
        return readJsonToolResult(response);
    }

    /** SyncMcpToolCallback은 MCP content 배열을 JSON으로 반환한다. 그 안의 JSON 텍스트를 읽는다. */
    public JsonNode readJsonToolResult(String response) {
        JsonNode content = objectMapper.readTree(response);
        if (!content.isArray() || content.size() != 1 || !content.get(0).has("text")) {
            throw new IllegalStateException("이 실습은 JSON 텍스트 하나인 MCP 결과를 사용합니다.");
        }
        String json = content
                .get(0)
                .path("text")
                .asString();
        return objectMapper.readTree(json);
    }
}
