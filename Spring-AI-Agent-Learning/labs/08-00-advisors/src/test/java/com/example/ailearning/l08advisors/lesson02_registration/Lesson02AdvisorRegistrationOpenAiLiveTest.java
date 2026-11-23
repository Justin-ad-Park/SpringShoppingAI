package com.example.ailearning.l08advisors.lesson02_registration;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/** 08-2: 실제 OpenAI로 요청별 추가가 기본 등록을 교체하지 않는지 확인한다. API 사용량이 발생한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.ai.openai.timeout=30s")
@ExtendWith(OutputCaptureExtension.class)
@Timeout(180)
class Lesson02AdvisorRegistrationOpenAiLiveTest {
    @Autowired
    Lesson02AdvisorRegistrationService service;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 실제_모델에서도_기본_등록_호출(CapturedOutput logs) {
        // When: 첫 요청에는 추가 로거를 전달하고, 다음 요청에는 기본 설정만 사용한다.
        String secondAnswer = service.chatWithDefaults("상품 문의에 필요한 정보는 무엇인가요?");

        // Then: 모델 응답은 두 번, 로그는 첫 요청 2번 + 다음 요청 1번이다.
        assertThat(secondAnswer).isNotBlank();
        long loggedRequests = logs.getOut().lines()
                .filter(line -> line.contains("SimpleLoggerAdvisor") && line.contains("request: "))
                .count();
        assertThat(loggedRequests).isEqualTo(1);

        System.out.printf("첫 답변: %s%n요청 로그 수: %d%n", secondAnswer, loggedRequests);
    }

    @Test
    void 실제_모델에서도_기본_등록과_요청별_추가의_범위가_구분된다(CapturedOutput logs) {
        // When: 첫 요청에는 추가 로거를 전달하고, 다음 요청에는 기본 설정만 사용한다.
        String firstAnswer = service.chatWithAdditionalAdvisor("안녕하세요.", SimpleLoggerAdvisor.builder().build());
        String secondAnswer = service.chatWithDefaults("상품 문의에 필요한 정보는 무엇인가요?");

        // Then: 모델 응답은 두 번, 로그는 첫 요청 2번 + 다음 요청 1번이다.
        assertThat(firstAnswer).isNotBlank();
        assertThat(secondAnswer).isNotBlank();
        long loggedRequests = logs.getOut().lines()
                .filter(line -> line.contains("SimpleLoggerAdvisor") && line.contains("request: "))
                .count();
        assertThat(loggedRequests).isEqualTo(3);
        System.out.printf("첫 답변: %s%n두 번째 답변: %s%n요청 로그 수: %d%n", firstAnswer, secondAnswer, loggedRequests);
    }


}
