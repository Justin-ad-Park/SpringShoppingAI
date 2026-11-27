package com.example.ailearning.l13mcpshopping.lesson12_mcpagent;

import com.example.shopping.mcp.McpApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/** 기존 shopping-mcp 애플리케이션을 별도 Spring 컨텍스트와 임의 포트에서 실행한다. */
final class ShoppingMcpTestServer implements AutoCloseable {
    private final ConfigurableApplicationContext context = new SpringApplicationBuilder(McpApplication.class)
            .run("--spring.config.name=mcp-test-server", "--server.address=127.0.0.1", "--server.port=0",
                    "--server.shutdown=immediate",
                    "--spring.ai.mcp.server.name=learning-shopping", "--spring.ai.mcp.server.protocol=STREAMABLE",
                    "--spring.ai.mcp.server.type=SYNC", "--spring.ai.mcp.client.enabled=false",
                    "--spring.ai.openai.api-key=unused-in-mcp-server",
                    "--spring.ai.model.chat=none", "--spring.ai.model.embedding=none",
                    "--shopping.mcp.return-policy.enabled=true",
                    "--spring.main.banner-mode=off", "--logging.level.root=WARN");

    String getBaseUrl() {
        int port = ((WebServerApplicationContext) context).getWebServer().getPort();
        return "http://127.0.0.1:" + port;
    }

    @Override
    public void close() {
        context.close();
    }
}
