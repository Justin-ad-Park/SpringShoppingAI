package com.example.shopping.agent.execution;

import java.util.*;
import com.fasterxml.jackson.databind.*;
import org.springframework.ai.tool.ToolCallback;
import com.example.shopping.agent.ChatDtos.Trace;

/** Transport-only request scope: discovery, JSON normalization, limits and traces. */
public class McpSession {
    private final ObjectMapper mapper;
    private final Map<String,ToolCallback> tools = new HashMap<>();
    private final List<Trace> traces = new ArrayList<>();
    private int calls;
    public McpSession(ObjectMapper mapper, ToolCallback[] callbacks) {
        this.mapper = mapper;
        for (ToolCallback t : callbacks) tools.put(t.getToolDefinition().name(), t);
    }
    public synchronized <T> T call(String name, Object args, com.fasterxml.jackson.core.type.TypeReference<T> type) {
        if (++calls > 24) throw new IllegalStateException("도구 호출 한도 초과");
        ToolCallback tool = tools.get(name);
        if (tool == null) throw new IllegalStateException("등록되지 않은 MCP 도구: " + name);
        long start = System.nanoTime();
        try {
            JsonNode input = mapper.valueToTree(args);
            JsonNode result = normalize(mapper.readTree(tool.call(input.toString())));
            if (result.path("isError").asBoolean()) throw new IllegalStateException("MCP 도구 실행 실패");
            traces.add(new Trace(name, input, result, (System.nanoTime()-start)/1_000_000));
            return mapper.convertValue(result, type);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("MCP 응답 해석 실패", e);
        }
    }
    public List<Trace> traces() { return List.copyOf(traces); }
    private JsonNode normalize(JsonNode node) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (node == null) throw new IllegalStateException("빈 MCP 응답");
        if (node.isTextual()) {
            try { return normalize(mapper.readTree(node.asText())); }
            catch (com.fasterxml.jackson.core.JsonProcessingException ignored) { return node; }
        }
        if (node.path("isError").asBoolean()) return node;
        if (node.hasNonNull("structuredContent")) return normalize(node.get("structuredContent"));
        JsonNode content = node.has("content") ? node.get("content") : node;
        if (content.isArray() && content.size() == 1 && content.get(0).has("text")) return normalize(content.get(0).get("text"));
        return node;
    }
}
