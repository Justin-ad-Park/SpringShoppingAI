package com.example.shopping.tutor.support;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** 테스트가 소유한 shopping-mcp 프로세스만 시작하고 종료한다. 기존 8082 서버는 사용하지 않는다. */
public final class ShoppingMcpTestServer implements AutoCloseable {
    private final Process process;
    private final String baseUrl;

    private ShoppingMcpTestServer(Process process, String baseUrl) {
        this.process = process;
        this.baseUrl = baseUrl;
    }

    public static ShoppingMcpTestServer start(String logFilename) throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        Path log = Path.of("build/reports", logFilename);
        Files.createDirectories(log.getParent());
        Path jar = Path.of("../shopping-mcp/build/libs/shopping-mcp-0.1.0.jar")
                .toAbsolutePath()
                .normalize();

        ProcessBuilder builder = new ProcessBuilder(
                "java", "-jar", jar.toString(), "--server.port=" + port);
        // MCP 서버 자체는 API 키를 필요로 하지 않는다.
        builder.environment().remove("OPENAI_API_KEY");
        builder.redirectErrorStream(true).redirectOutput(log.toFile());

        ShoppingMcpTestServer server = new ShoppingMcpTestServer(
                builder.start(), "http://127.0.0.1:" + port);
        try {
            server.waitUntilReady();
            return server;
        } catch (Exception failure) {
            server.close();
            throw failure;
        }
    }

    public String baseUrl() {
        return baseUrl;
    }

    private void waitUntilReady() throws Exception {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/actuator/health"))
                .timeout(Duration.ofSeconds(1))
                .build();

        for (int attempt = 0; attempt < 80; attempt++) {
            if (!process.isAlive()) {
                throw new IllegalStateException("격리 MCP 서버가 종료되었습니다. build/reports 로그를 확인하세요.");
            }

            try {
                if (http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    return;
                }
            } catch (IOException notReady) {
                // 서버가 HTTP 포트를 열기 전의 연결 실패는 준비 시간 안에서 다시 확인한다.
            }
            Thread.sleep(250);
        }
        throw new IllegalStateException("격리 MCP 서버의 준비 시간이 초과되었습니다.");
    }

    @Override
    public void close() {
        process.destroy();
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException interrupted) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }
}
