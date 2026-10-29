package com.example.shopping.tutor.lesson.l10mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shopping.tutor.platform.config.AiModelConfiguration;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.adapter.McpCases;
import com.example.shopping.tutor.support.ShoppingMcpTestServer;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

/** L10-02: 실제 모델 → 실제 MCP 도구 → 도구 결과 → 실제 모델의 최종 답변. */
@Tag("live-mcp")
@SpringBootTest(properties = "tutor.mode=live")
class LiveMcpTest {
    private static ShoppingMcpTestServer mcpServer;

    @Autowired private AiModelConfiguration.Settings settings;
    @Autowired private ChatModel chatModel;
    @Autowired private EmbeddingModel embeddingModel;

    @BeforeAll
    static void startMcpServer() throws Exception {
        mcpServer = ShoppingMcpTestServer.start("live-mcp-server.log");
    }

    @AfterAll
    static void stopMcpServer() {
        if (mcpServer != null) {
            mcpServer.close();
        }
    }

    @Test
    void actualModelChoosesRemoteProductToolAndAnswersFromItsResults() throws Exception {
        AiModelConfiguration.Settings liveMcpSettings =
                new AiModelConfiguration.Settings(
                        "live",
                        settings.model(),
                        settings.embeddingModel(),
                        mcpServer.baseUrl(),
                        settings.nativeSchema());
        LessonExecution context =
                new LessonExecution(
                        "반드시 원격 상품 검색 도구로 15000원 이하 음료를 조회하고 상품 이름을 알려줘.",
                        "standard",
                        "normal",
                        "live-mcp-conversation",
                        Map.of(),
                        liveMcpSettings,
                        chatModel,
                        embeddingModel,
                        new LessonExecution.State(),
                        null);

        Outcome result = new McpCases().letModelUseRemoteTool(context);
        context.finish(new LessonExecution.State());
        writeObservationReport(context, result);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(context.toolCalls)
                .anySatisfy(
                        call -> {
                            assertThat(call.get("name").toString()).endsWith("searchProducts");
                            assertThat(call.get("result").toString()).contains("P004", "P005");
                        });
        assertThat(result.answer()).contains("허브티", "커피");
    }

    private void writeObservationReport(LessonExecution context, Outcome result) throws Exception {
        Path report = Path.of("build/reports/live-mcp-evaluation.json");
        Files.createDirectories(report.getParent());
        new ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValue(
                        report.toFile(),
                        Map.of(
                                "caseId",
                                "L10-02",
                                "executedAt",
                                Instant.now().toString(),
                                "model",
                                settings.model(),
                                "scope",
                                "실제 모델과 격리된 실제 MCP 서버의 통합 경로",
                                "answer",
                                result.answer(),
                                "diagnostics",
                                context.diagnostics));
    }
}
