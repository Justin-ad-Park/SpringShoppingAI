package com.example.ailearning.l08advisors.lesson07_looporder;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import java.util.Collections;
import com.example.ailearning.l08advisors.lesson06_toolloop.PriceTools;
import com.example.ailearning.testsupport.output.ToolCallPrinter;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.mockito.Mockito.*;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 OpenAI에 연결하며 API 사용량이 발생한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.ai.openai.timeout=30s")
@ExtendWith(OutputCaptureExtension.class)
@Timeout(180)
class Lesson07ToolLoopOrderOpenAiLiveTest {
    @Autowired
    Lesson07ToolLoopOrderChatService service;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @MockitoSpyBean
    PriceTools priceTools;

    @Test
    void 실제_도구_반복에서도_전체_요청과_각_모델_요청의_기록을_구분한다(CapturedOutput logs) {
        String answer = service.chat("허브티의 상품 가격은 얼마인가요?");
        var steps = logs.getOut().lines().filter(line -> line.matches("(request|modelA|modelB):(before|after)")).toList();
        System.out.println("답변: " + answer);
        ToolCallPrinter.printToolCalls(priceTools);

        verify(priceTools, atLeastOnce()).getTeaPrice(any());
        assertThat(answer).isNotBlank();
        assertThat(steps).startsWith("request:before").endsWith("request:after");
        assertThat(Collections.frequency(steps, "request:before")).isEqualTo(1);
        assertThat(Collections.frequency(steps, "request:after")).isEqualTo(1);
        int modelRequests = Collections.frequency(steps, "modelA:before");
        // 실제 모델이 도구를 다시 요청할 수 있으므로 반복 횟수는 고정하지 않는다.
        assertThat(modelRequests).isGreaterThanOrEqualTo(2);
        assertThat(Collections.frequency(steps, "modelB:after")).isEqualTo(modelRequests);
    }
}
