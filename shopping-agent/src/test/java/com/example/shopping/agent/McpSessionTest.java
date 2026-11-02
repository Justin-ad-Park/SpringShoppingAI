package com.example.shopping.agent;

import java.util.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;
import com.example.shopping.agent.execution.McpSession;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class McpSessionTest {
    @Test void unwrapsMcpContentWithoutInjectingTaskSpecificArguments() throws Exception {
        var mapper = new ObjectMapper();
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(ToolDefinition.builder().name("searchProducts").description("test").inputSchema("{}").build());
        String content = mapper.writeValueAsString(List.of(Map.of("type","text","text","{\"count\":1}")));
        when(callback.call(anyString())).thenReturn(content);
        McpSession session = new McpSession(mapper,new ToolCallback[]{callback});
        Map<String,Integer> result = session.call("searchProducts",Map.of("query",Map.of("maxPrice",30000)),new TypeReference<>(){});
        assertThat(result.get("count")).isEqualTo(1);
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(callback).call(captor.capture());
        assertThat(mapper.readTree(captor.getValue()).has("scenario")).isFalse();
        assertThat(session.traces()).hasSize(1);
        assertThat(new McpSession(mapper,new ToolCallback[]{callback}).traces()).isEmpty();
        for(int i=1;i<24;i++) session.call("searchProducts",Map.of(),new TypeReference<Map<String,Integer>>(){});
        assertThatThrownBy(() -> session.call("searchProducts",Map.of(),new TypeReference<Map<String,Integer>>(){})).isInstanceOf(IllegalStateException.class);
    }
}
