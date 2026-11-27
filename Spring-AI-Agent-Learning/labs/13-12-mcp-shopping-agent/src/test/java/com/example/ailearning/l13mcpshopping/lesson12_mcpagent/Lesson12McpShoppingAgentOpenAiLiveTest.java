package com.example.ailearning.l13mcpshopping.lesson12_mcpagent;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** 실제 OpenAI와 테스트가 기동한 실제 shopping-mcp를 연결한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.ai.mcp.server.enabled=false", "spring.ai.openai.timeout=30s"})
@Timeout(120)
class Lesson12McpShoppingAgentOpenAiLiveTest {
    private static final ShoppingMcpTestServer MCP_SERVER = new ShoppingMcpTestServer();

    @Autowired Lesson12McpShoppingAgent shoppingAgent;
    @Autowired McpToolCallLog toolCallLog;

    @DynamicPropertySource
    static void MCP_서버_주소_설정(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.mcp.client.streamable-http.connections.shopping.url", MCP_SERVER::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        toolCallLog.toolNames.clear();
    }

    @AfterAll
    static void 서버_종료() {
        MCP_SERVER.close();
    }

    @Test
    void 실제_OpenAI가_MCP에서_상품_총액과_미개봉_반품_기간을_조회한다() {
        String answer = shoppingAgent.answer("3만원 이하 음료를 추천하고 미개봉 반품 기간도 알려줘.", List.of());

        System.out.println(answer);
        verifyShoppingAndReturnAnswer(answer);
    }

    @Test
    void 실제_OpenAI가_확인_질문을_하고_고객이_답하면_MCP_조회를_이어서_처리한다() {
        String firstQuestion = "음료 추천해 주고 반품 기간도 알려줘.";
        String clarification = shoppingAgent.answer(firstQuestion, List.of());

        System.out.println("확인 질문: " + clarification);
        assertThat(clarification).containsAnyOf("예산", "얼마");
        assertThat(clarification).containsAnyOf("개봉", "미개봉");

        toolCallLog.toolNames.clear();
        List<Message> previousMessages = List.of(new UserMessage(firstQuestion), new AssistantMessage(clarification));
        String answer = shoppingAgent.answer("배송비 포함 3만원이고, 개봉하지 않았어요.", previousMessages);

        System.out.println("이어서 받은 답변:\n" + answer);
        verifyShoppingAndReturnAnswer(answer);
    }

    @Test
    void 실제_OpenAI가_반품_기간만_물으면_MCP의_반품_도구만_사용한다() {
        String answer = shoppingAgent.answer("미개봉 반품 기간만 알려줘.", List.of());

        System.out.println(answer);
        System.out.println("실행한 MCP 도구: " + toolCallLog.toolNames);
        assertThat(toolCallLog.toolNames).containsOnly("getReturnPolicy");
        assertReturnPolicyAnswer(answer);
    }

    private void verifyShoppingAndReturnAnswer(String answer) {
        System.out.println("실행한 MCP 도구: " + toolCallLog.toolNames);
        assertThat(toolCallLog.toolNames).contains("searchProducts", "quoteCart", "getReturnPolicy");
        assertThat(answer).containsAnyOf("허브티", "드립 커피");
        assertThat(answer).containsPattern("(?:12[,]?900|15[,]?500)\\s*원");
        assertReturnPolicyAnswer(answer);
    }

    private static void assertReturnPolicyAnswer(String answer) {
        assertThat(answer).containsAnyOf("수령", "받은");
        assertThat(answer).containsPattern("7\\s*일|일주일");
        assertThat(answer).contains("return-sealed");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ToolObservationConfiguration {
        @Bean
        McpToolCallLog mcpToolCallLog() {
            return new McpToolCallLog();
        }
    }

    // 이 실습에는 MCP 도구만 등록한다. Spring AI의 도구 실행이 끝날 때 이름을 남긴다.
    static final class McpToolCallLog implements ObservationHandler<ToolCallingObservationContext> {
        private final List<String> toolNames = new CopyOnWriteArrayList<>();

        @Override
        public boolean supportsContext(Observation.Context context) {
            return context instanceof ToolCallingObservationContext;
        }

        @Override
        public void onStop(ToolCallingObservationContext context) {
            if (context.getError() == null) {
                toolNames.add(context.getToolDefinition().name());
            }
        }
    }
}
