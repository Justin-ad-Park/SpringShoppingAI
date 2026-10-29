package com.example.shopping.integration.support;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** 실행한 자원을 역순으로 정리한다. 서비스 시작 중 실패해도 같은 정리 경로를 사용한다. */
public final class TestEnvironment implements AutoCloseable {
    private final Path root;
    private final ArrayDeque<AutoCloseable> resources = new ArrayDeque<>();
    private String baseUrl;
    private OpenAiStub stub;

    TestEnvironment(Path root, String baseUrl) {
        this.root = root;
        this.baseUrl = baseUrl;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public OpenAiStub stub() {
        return stub;
    }

    OpenAiStub startStub() throws IOException {
        stub = new OpenAiStub();
        resources.push(stub);
        return stub;
    }

    void startServices(TestMode strategy) throws Exception {
        var agentOptions = new ArrayList<>(strategy.agentOptions(this));
        int mcpPort = startService("shopping-mcp", List.of());

        agentOptions.add("--spring.ai.mcp.client.streamable-http.connections.shopping.url=http://127.0.0.1:" + mcpPort);
        int agentPort = startService("shopping-agent", agentOptions);

        int clientPort = startService("chatbot-client", List.of("--agent.url=http://127.0.0.1:" + agentPort));
        baseUrl = "http://127.0.0.1:" + clientPort;
    }

    private int startService(String module, List<String> options) throws Exception {
        int port = availablePort();
        Path logs = Files.createDirectories(root.resolve(".run"));
        Path log = logs.resolve(module + "-java-test-" + port + ".log");
        Path jar = root.resolve(module + "/build/libs/" + module + "-0.1.0.jar");
        if (!Files.isRegularFile(jar)) {
            throw new IOException("먼저 bootJar로 빌드하세요: " + jar);
        }

        var command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-jar", jar.toString(), "--server.port=" + port,
                "--spring.lifecycle.timeout-per-shutdown-phase=2s"));
        command.addAll(options);

        var builder = new ProcessBuilder(command).directory(root.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().remove("SPRING_APPLICATION_JSON");
        Process process = builder.start();
        resources.push(() -> stop(process));
        awaitReady(port, process, log);
        return port;
    }

    private static int availablePort() throws IOException {
        // OS가 빈 포트를 선택한다. 소켓을 닫은 뒤 실제 서비스가 포트를 사용한다.
        try (var socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    private static void awaitReady(int port, Process process, Path log) throws Exception {
        try (var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            for (int attempt = 0; attempt < 90; attempt++) {
                if (!process.isAlive()) {
                    throw new IOException("서비스 종료: " + log);
                }
                try {
                    var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
                            .timeout(Duration.ofSeconds(1)).GET().build();
                    if (http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                        return;
                    }
                } catch (IOException notReady) {
                    // 기동 중 연결 거절이나 일시적인 응답 지연은 다음 회차에 재확인한다.
                }
                Thread.sleep(1000);
            }
        }
        throw new IOException("서비스 준비 시간 초과: " + log);
    }

    private static void stop(Process process) throws InterruptedException {
        process.destroy();
        try {
            if (!process.waitFor(8, TimeUnit.SECONDS)) {
                process.destroyForcibly().waitFor();
            }
        } catch (InterruptedException interrupted) {
            process.destroyForcibly();
            throw interrupted;
        }
    }

    @Override
    public void close() throws Exception {
        Exception failure = null;
        while (!resources.isEmpty()) {
            try {
                resources.pop().close();
            } catch (Exception error) {
                if (failure == null) {
                    failure = error;
                } else {
                    failure.addSuppressed(error);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
