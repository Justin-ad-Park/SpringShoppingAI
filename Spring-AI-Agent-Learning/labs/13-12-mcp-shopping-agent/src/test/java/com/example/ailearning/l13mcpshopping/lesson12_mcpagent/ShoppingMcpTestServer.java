package com.example.ailearning.l13mcpshopping.lesson12_mcpagent;

import com.example.shopping.mcp.McpApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/** 기존 shopping-mcp 애플리케이션을 별도 Spring 컨텍스트와 임의 포트에서 실행한다. */
final class ShoppingMcpTestServer implements AutoCloseable {
    private final ConfigurableApplicationContext context = new SpringApplicationBuilder(McpApplication.class)
            .run("--spring.config.name=mcp-test-server", "--server.address=127.0.0.1",
                    // 포트에 0을 지정하면 서버 시작 시 운영체제가 사용 가능한 포트를 배정한다.
                    "--server.port=0",
                    "--server.shutdown=immediate",
                    "--spring.ai.mcp.server.name=learning-shopping", "--spring.ai.mcp.server.protocol=STREAMABLE",
                    "--spring.ai.mcp.server.type=SYNC", "--spring.ai.mcp.client.enabled=false",
                    "--spring.ai.openai.api-key=unused-in-mcp-server",
                    "--spring.ai.model.chat=none", "--spring.ai.model.embedding=none",
                    "--shopping.mcp.return-policy.enabled=true",
                    "--spring.main.banner-mode=off", "--logging.level.root=WARN");

    /** 서버에 배정된 실제 포트를 읽어 테스트의 MCP 클라이언트에 전달할 주소를 만든다. */
    String getBaseUrl() {
        int port = ((WebServerApplicationContext) context).getWebServer().getPort();
        return "http://127.0.0.1:" + port;
    }

    @Override
    public void close() {
        context.close();
    }
}
