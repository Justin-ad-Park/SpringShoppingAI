package com.example.shopping.tutor.lesson.l10mcp;

import static org.assertj.core.api.Assertions.*;

import io.modelcontextprotocol.spec.McpSchema;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

@Tag("mcp")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"tutor.mode=stub", "tutor.api-key=", "spring.ai.mcp.server.enabled=true"})
class PolicyMcpServerTest {
    @LocalServerPort int port;

    @Test
    void L1004_exposesTutorPolicyToolOverActualStreamableHttp() {
        try (var client =
                new com.example.shopping.tutor.lesson.l10mcp.McpLesson()
                        .connect("http://127.0.0.1:" + port)) {
            assertThat(client.listTools().tools()).anyMatch(t -> t.name().equals("getTutorPolicy"));
            var result = client.callTool(new McpSchema.CallToolRequest("getTutorPolicy", Map.of()));
            assertThat(result.isError()).isNotEqualTo(true);
            assertThat(result.toString()).contains("7", "virtual");
        }
    }
}
