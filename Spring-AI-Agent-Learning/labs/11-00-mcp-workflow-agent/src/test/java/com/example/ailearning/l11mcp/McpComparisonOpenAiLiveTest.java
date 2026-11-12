package com.example.ailearning.l11mcp;

import com.example.ailearning.testsupport.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

/** MCP 서버는 자동 실행하며 OpenAI만 실제 외부 서비스에 연결한다. */
@SpringBootTest(properties = "spring.ai.mcp.server.enabled=false")
class McpComparisonOpenAiLiveTest {
    private static final McpTestServer MCP = new McpTestServer();
    private static final String QUESTION = "음료 중 상품 가격 15000원 이하인 MALL 상품을 모두 한 개씩 구매할 때 배송비 포함 총액을 알려줘. 총예산은 30000원이야.";
    @Autowired McpWorkflowService workflowService;
    @Autowired McpAgentService agentService;
    @Autowired McpHybridService hybridService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.mcp.client.streamable-http.connections.shopping.url", MCP::getBaseUrl);
    }

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @AfterAll
    static void 서버_종료() {
        MCP.close();
    }

    @Test
    @Timeout(120)
    void 실제_OpenAI로_조건을_추출하고_Workflow가_MCP를_호출한다() {
        McpRunResult result = workflowService.run(QUESTION);
        System.out.println(result);
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.toolCalls()).hasSize(2);
        assertThat(result.toolCalls().getLast().result().path("total").asLong()).isEqualTo(25400);
    }

    @Test
    @Timeout(120)
    void 실제_OpenAI가_MCP_검색과_계산_도구를_선택한다() {
        McpRunResult result = agentService.run(QUESTION);
        System.out.println(result);
        assertThat(result.answer()).isNotBlank();
        assertThat(result.toolCalls()).extracting(McpRunResult.ToolCall::name).contains("searchProducts", "quoteCart");
        assertThat(result.toolCalls().getLast().result().path("total").asLong()).isEqualTo(25400);
    }

    @Test
    @Timeout(120)
    void 실제_OpenAI의_도구_선택_후_Java가_총예산을_검증한다() {
        McpRunResult result = hybridService.run(QUESTION);
        System.out.println(result);
        assertThat(result.status()).isEqualTo("VERIFIED");
        assertThat(result.answer()).contains("25400");
    }
}
