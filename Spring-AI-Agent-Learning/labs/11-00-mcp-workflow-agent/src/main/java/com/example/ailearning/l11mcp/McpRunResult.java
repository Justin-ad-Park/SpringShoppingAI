package com.example.ailearning.l11mcp;

import tools.jackson.databind.JsonNode;
import java.util.List;

public record McpRunResult(String status, String answer, List<ToolCall> toolCalls) {
    public record ToolCall(String name, JsonNode arguments, JsonNode result) {}
}
