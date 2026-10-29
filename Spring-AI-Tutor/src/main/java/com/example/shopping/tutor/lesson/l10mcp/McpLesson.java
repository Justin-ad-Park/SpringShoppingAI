package com.example.shopping.tutor.lesson.l10mcp;

import com.example.shopping.tutor.lesson.l06tools.support.ShippingQuoteCalculator;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.util.*;

/** 10 단원: 핵심 API를 직접 호출한다. 웹 실행·세션 확정은 platform에서 담당한다. */
public class McpLesson {

    /** 연결을 선택한 케이스에서만 생성하고 닫는다. 기본 시작은 MCP에 의존하지 않는다. */

    /** 핵심 동작을 분리해 단원 테스트에서 직접 검증한다. */
    public McpSyncClient connect(String url) {
        var transport = HttpClientStreamableHttpTransport.builder(url).endpoint("/mcp").build();
        var client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
        try {
            client.initialize();
            return client;
        } catch (RuntimeException e) {
            client.closeGracefully();
            throw new IllegalStateException("MCP 연결 실패. shopping-mcp 실행과 TUTOR_MCP_URL을 확인하세요.", e);
        }
    }

    /** 실제 Streamable HTTP 서버의 schema와 호출 결과를 확인합니다. */
    public Result discoverAndCall(String mcpUrl) {
        try (var client = connect(mcpUrl)) {
            var tools = client.listTools().tools();
            var result =
                    client.callTool(new McpSchema.CallToolRequest("getShippingPolicy", Map.of()));
            if (Boolean.TRUE.equals(result.isError())) throw new IllegalStateException("MCP 도구 오류");
            return Result.ok(
                    "원격 MCP 도구를 발견하고 배송 정책을 조회했습니다.", Map.of("tools", tools, "result", result));
        }
    }

    /** 허용한 MCP callback만 ChatClient에 등록합니다. */
    public Result letModelUseRemoteTool(
            ChatClient chatClient,
            String question,
            String mcpUrl,
            java.util.function.Function<ToolCallback[], ToolCallback[]> decorateCallbacks) {
        try (var client = connect(mcpUrl)) {
            ToolCallback[] callbacks =
                    Arrays.stream(new SyncMcpToolCallbackProvider(client).getToolCallbacks())
                            .filter(t -> t.getToolDefinition().name().endsWith("searchProducts"))
                            .toArray(ToolCallback[]::new);
            if (callbacks.length != 1) throw new IllegalStateException("상품 검색 도구를 하나만 허용해야 합니다.");
            return Result.text(
                    chatClient
                            .prompt()
                            .toolCallbacks(decorateCallbacks.apply(callbacks))
                            .user(question)
                            .call()
                            .content());
        }
    }

    /** 같은 fixture의 사실과 transport 오류를 구분합니다. */
    public Result compareLocalAndRemote(String fixture, String mcpUrl) {
        try (var client = connect(mcpUrl)) {
            var result =
                    client.callTool(
                            new McpSchema.CallToolRequest(
                                    "quoteCart",
                                    Map.of(
                                            "scenario",
                                            fixture,
                                            "additionalProductIds",
                                            List.of("P004"))));
            if (Boolean.TRUE.equals(result.isError())) throw new IllegalStateException("원격 견적 실패");
            return Result.ok(
                    "로컬 계산과 원격 응답을 비교하세요.",
                    Map.of(
                            "local",
                            ShippingQuoteCalculator.quote(fixture, List.of("P004")),
                            "remote",
                            result));
        }
    }

    /** 선택 서버 모드에서 @Tool을 MCP로 노출합니다. */
    public Result exposeTutorPolicyTool(
            boolean serverEnabled,
            int serverPort,
            org.springframework.core.env.Environment environment) {
        if (!serverEnabled)
            return Result.unsupported(
                    "선택 MCP 서버는 scripts/run.sh --mcp-server로 실행하세요. Tutor의 /mcp에 가상 정책 도구를 노출합니다.");
        int port = environment.getProperty("local.server.port", Integer.class, serverPort);
        try (var client = connect("http://127.0.0.1:" + port)) {
            var result = client.callTool(new McpSchema.CallToolRequest("getTutorPolicy", Map.of()));
            if (Boolean.TRUE.equals(result.isError()))
                throw new IllegalStateException("Tutor 정책 도구 실패");
            return Result.ok(
                    "Tutor의 @Tool을 MCP로 발견하고 호출했습니다.",
                    Map.of("tools", client.listTools().tools(), "result", result));
        }
    }

    public record Result(String status, String answer, Map<String, Object> data) {
        public static Result ok(String answer, Map<String, Object> data) {
            return new Result("COMPLETED", answer, data);
        }

        public static Result text(String answer) {
            if (answer == null || answer.isBlank())
                throw new IllegalStateException("모델이 빈 응답을 반환했습니다.");
            return ok(answer, Map.of());
        }

        public static Result unsupported(String reason) {
            return new Result("UNSUPPORTED", reason, Map.of());
        }
    }
}
