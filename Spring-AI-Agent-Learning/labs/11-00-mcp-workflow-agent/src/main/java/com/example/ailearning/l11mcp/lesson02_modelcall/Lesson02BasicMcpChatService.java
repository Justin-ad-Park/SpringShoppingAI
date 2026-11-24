package com.example.ailearning.l11mcp.lesson02_modelcall;

import io.modelcontextprotocol.client.McpSyncClient;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

/** 11-2: 제공되는 Provider로 MCP 도구를 모델에 연결한다. 자체 실행기를 사용하지 않는다. */
@Service
public class Lesson02BasicMcpChatService {
    private final ChatClient chatClient;
    private final SyncMcpToolCallbackProvider toolProvider;

    public Lesson02BasicMcpChatService(ChatClient.Builder builder, List<McpSyncClient> mcpClients) {
        chatClient = builder.build();
        toolProvider = SyncMcpToolCallbackProvider.builder()
                .mcpClients(mcpClients)
                .build();
    }

    public String ask(String question) {
        return chatClient
                .prompt()
                .user(question)
                .toolCallbacks(toolProvider)
                .call()
                .content();
    }

    public ToolCallback[] availableTools() {
        return toolProvider.getToolCallbacks();
    }
}
