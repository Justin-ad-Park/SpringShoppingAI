package com.example.ailearning.l11mcp;

import com.example.shopping.mcp.McpApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/** shopping-mcp를 임의 포트의 실제 HTTP 서버로 실행하는 테스트 전용 준비 코드. */
final class McpTestServer implements AutoCloseable {
    private final ConfigurableApplicationContext context = new SpringApplicationBuilder(McpApplication.class)
            .run("--spring.config.name=mcp-test-server", "--server.address=127.0.0.1", "--server.port=0", "--server.shutdown=immediate",
                    "--spring.ai.mcp.server.name=learning-shopping",
                    "--spring.ai.mcp.server.protocol=STREAMABLE", "--spring.ai.mcp.server.type=SYNC",
                    "--spring.ai.mcp.client.enabled=false",
                    "--spring.ai.openai.api-key=unused-in-mcp-server", "--spring.ai.model.chat=none", "--spring.ai.model.embedding=none",
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
