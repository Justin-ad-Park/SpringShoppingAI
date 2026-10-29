package com.example.shopping.tutor.lesson.l10mcp;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l07vector.support.FixedEmbeddingModel;
import com.example.shopping.tutor.platform.config.AiModelConfiguration;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.stub.ScriptedChatModel;
import com.example.shopping.tutor.platform.web.adapter.McpCases;

import org.junit.jupiter.api.*;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

@Tag("mcp")
class McpIntegrationTest {
    static Process server;
    static String url;

    @BeforeAll
    static void startIsolatedShoppingMcp() throws Exception {
        int port;
        try (var socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        url = "http://127.0.0.1:" + port;
        Path log = Path.of("build/reports/mcp-server.log");
        Files.createDirectories(log.getParent());
        var builder =
                new ProcessBuilder(
                        "java",
                        "-jar",
                        Path.of("../shopping-mcp/build/libs/shopping-mcp-0.1.0.jar")
                                .toAbsolutePath()
                                .normalize()
                                .toString(),
                        "--server.port=" + port);
        builder.environment().remove("OPENAI_API_KEY");
        builder.redirectErrorStream(true).redirectOutput(log.toFile());
        server = builder.start();
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
        for (int i = 0; i < 80; i++) {
            if (!server.isAlive())
                throw new IllegalStateException("격리 MCP 시작 실패. build/reports/mcp-server.log 확인");
            try {
                if (http.send(
                                        HttpRequest.newBuilder(URI.create(url + "/actuator/health"))
                                                .timeout(Duration.ofSeconds(1))
                                                .build(),
                                        HttpResponse.BodyHandlers.discarding())
                                .statusCode()
                        == 200) return;
            } catch (java.io.IOException ignored) {
            }
            Thread.sleep(250);
        }
        stop();
        throw new IllegalStateException("격리 MCP 준비 시간 초과");
    }

    @AfterAll
    static void stop() throws Exception {
        if (server != null) {
            server.destroy();
            if (!server.waitFor(10, java.util.concurrent.TimeUnit.SECONDS))
                server.destroyForcibly();
        }
    }

    LessonExecution context(String message) {
        return new LessonExecution(
                message,
                "standard",
                "normal",
                "mcp-test",
                Map.of(),
                new AiModelConfiguration.Settings("stub", "scripted", "fixed", url, false),
                new ScriptedChatModel(),
                new FixedEmbeddingModel(),
                new LessonExecution.State(),
                null);
    }

    @Test
    void L1001_discoversSchemaAndCallsRealServer() {
        var out = new McpCases().discoverAndCall(context("정책 조회"));
        assertThat(out.data().toString()).contains("searchProducts", "50000");
    }

    @Test
    void L1002_actualMcpCallbackPassesThroughModelToolLoop() {
        var c = context("15000원 이하 음료");
        new McpCases().letModelUseRemoteTool(c);
        assertThat(c.toolCalls).hasSize(1);
        assertThat(c.toolCalls.toString()).contains("P004", "P005");
    }

    @Test
    void L1003_localAndRemoteQuoteAgree() {
        var out = new McpCases().compareLocalAndRemote(context("견적"));
        assertThat(out.data().toString()).contains("51900");
    }

    @Test
    void connectionFailureIsNotNoResults() {
        assertThatThrownBy(
                        () ->
                                new com.example.shopping.tutor.lesson.l10mcp.McpLesson()
                                        .connect("http://127.0.0.1:1"))
                .isInstanceOf(IllegalStateException.class);
    }
}
