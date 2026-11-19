package com.example.ailearning.l08advisors.logging;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/** 08-1: 실제 OpenAI를 호출한다. 루트 .env가 필요하며 API 사용량이 발생한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.ai.openai.timeout=30s")
@ExtendWith(OutputCaptureExtension.class)
@Timeout(180)
class Lesson01LoggingOpenAiLiveTest {
    @Autowired
    Lesson01LoggingChatService service;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 실제_모델의_요청과_응답을_기본_로거로_확인한다(CapturedOutput logs) {
        // When
        String answer = service.chat("안녕하세요. 상품 문의를 하려면 어떤 정보를 준비하면 좋을까요?");

        // Then: 아래 출력문을 실행하기 전에 Advisor의 로그부터 확인한다.
        assertThat(answer).isNotBlank();
        assertThat(logs.getOut()).contains("request: ", "response: ");
        System.out.printf("실제 답변: %s%n", answer);
    }
}
