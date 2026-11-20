package com.example.ailearning.l08advisors.toolloop;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.ailearning.testsupport.output.ToolCallPrinter;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

/** 실제 OpenAI를 호출한다. 루트 .env가 필요하며 API 사용량이 발생한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.ai.openai.timeout=30s")
@Timeout(180)
class Lesson05ToolLoopAdvisorOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired
    Lesson05ToolLoopChatService service;

    @MockitoSpyBean
    PriceTools priceTools;

    @Test
    void 실제_가격_도구_실행과_도구_루프_안팎의_기록을_확인한다() {
        // When: 실제 고객의 가격 질문으로 도구 선택을 유도한다.
        var reply = service.chat("허브티의 상품 가격은 얼마인가요?");
        System.out.printf("답변: %s%n", reply.answer());
        // 서비스에서 모은 executionSteps는 응답의 steps()로 전달된다.
        System.out.println("Advisor 실행 순서:");
        printCallSteps(reply.steps());
        ToolCallPrinter.printToolCalls(priceTools);

        // Then: 도구 사용은 답변 문장으로 추정하지 않고 실제 실행 기록으로 검증한다.
        verify(priceTools, atLeastOnce()).getTeaPrice();
        assertThat(reply.answer()).isNotBlank();
        assertThat(reply.steps()).startsWith("request:before").endsWith("request:after");
        assertThat(Collections.frequency(reply.steps(), "request:before")).isEqualTo(1);
        assertThat(Collections.frequency(reply.steps(), "request:after")).isEqualTo(1);
        int modelRequests = Collections.frequency(reply.steps(), "model:before");
        // 모델은 도구를 재요청할 수 있으므로 정확히 2회라고 고정하지 않는다.
        assertThat(modelRequests).isGreaterThanOrEqualTo(2);
        assertThat(Collections.frequency(reply.steps(), "model:after")).isEqualTo(modelRequests);
    }

    private static void printCallSteps(List<String> steps) {
        for (int index = 0; index < steps.size(); index++) {
            System.out.printf("%d. %s%n", index + 1, steps.get(index));
        }
    }
}
