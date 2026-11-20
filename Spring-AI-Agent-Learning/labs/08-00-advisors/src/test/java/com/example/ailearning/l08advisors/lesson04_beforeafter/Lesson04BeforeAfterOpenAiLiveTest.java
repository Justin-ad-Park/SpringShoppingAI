package com.example.ailearning.l08advisors.lesson04_beforeafter;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 OpenAI에 연결하며 API 사용량이 발생한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.ai.openai.timeout=30s")
@ExtendWith(OutputCaptureExtension.class)
@Timeout(180)
class Lesson04BeforeAfterOpenAiLiveTest {
    @Autowired
    Lesson04BeforeAfterChatService service;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 실제_모델_호출에서_Advisor_하나가_요청_전과_정상_응답_후에_실행된다(CapturedOutput logs) {
        String answer = service.chat("안녕하세요.");
        var steps = logs.getOut().lines().filter(line -> line.startsWith("single:")).toList();
        System.out.println("답변: " + answer);
        assertThat(answer).isNotBlank();
        assertThat(steps).containsExactly("single:before", "single:after");
    }
}
