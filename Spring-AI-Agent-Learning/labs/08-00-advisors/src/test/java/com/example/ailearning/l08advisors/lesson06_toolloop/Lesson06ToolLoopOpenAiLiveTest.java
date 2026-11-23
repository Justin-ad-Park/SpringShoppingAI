package com.example.ailearning.l08advisors.lesson06_toolloop;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.ailearning.testsupport.output.ToolCallPrinter;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;


import static org.mockito.Mockito.*;

/** 실제 OpenAI를 호출한다. 루트 .env가 필요하며 API 사용량이 발생한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.ai.openai.timeout=30s")
@Timeout(180)
class Lesson06ToolLoopOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired
    Lesson06ToolLoopChatService service;

    @MockitoSpyBean
    PriceTools priceTools;

    @Test
    void 실제_가격_도구의_결과로_답변을_받는다() {
        // When: 실제 고객의 가격 질문으로 도구 선택을 유도한다.
        String answer = service.chat("허브티의 상품 가격은 얼마인가요?");
        System.out.printf("답변: %s%n", answer);
        ToolCallPrinter.printToolCalls(priceTools);

        // Then: 도구 사용은 답변 문장으로 추정하지 않고 실제 실행 기록으로 검증한다.
        verify(priceTools, atLeastOnce()).getTeaPrice(any());
        assertThat(answer).isNotBlank();
    }
}
