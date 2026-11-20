package com.example.ailearning.l08advisors.lesson02_registration;

import com.example.ailearning.testsupport.model.OpenAiStubServer;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** 08-2: 메모리나 A/B 순서 대신 기본 등록과 요청별 추가의 적용 범위만 비교한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.config.import=")
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class Lesson02AdvisorRegistrationLocalModelTest {
    private static final OpenAiStubServer MODEL = new OpenAiStubServer();

    @Autowired
    Lesson02AdvisorRegistrationService service;

    @DynamicPropertySource
    static void modelAddress(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL.chat().resetResponsesAndRequests();
        MODEL.chat().setResponseText("고정 응답");
    }

    @AfterAll
    static void 모델_대역_종료() {
        MODEL.close();
    }

    @Test
    void 기본_등록한_Advisor는_요청에서_다시_넘기지_않아도_매번_실행한다(CapturedOutput logs) {
        // When
        service.chatWithDefaults("첫 번째 질문");
        service.chatWithDefaults("두 번째 질문");

        // Then: 두 요청에 기본 로거가 각각 한 번 실행된다.
        assertThat(loggedRequests(logs)).isEqualTo(2);
        assertThat(MODEL.chat().getRequests()).hasSize(2);
    }

    @Test
    void 요청별_Advisor는_기본_Advisor에_추가되고_다음_요청에는_남지_않는다(CapturedOutput logs) {
        // Given: 실제 기본 로거의 동작을 유지하며 호출만 기록하는 Spy
        var additionalLogger = spy(SimpleLoggerAdvisor.builder().build());

        // When: 기본 로거 + 이번 요청의 추가 로거
        service.chatWithAdditionalAdvisor("추가 로그가 필요한 질문", additionalLogger);

        // Then: 교체가 아니라 추가이므로 한 모델 요청에서 로그가 두 번 남는다.
        assertThat(loggedRequests(logs)).isEqualTo(2);

        // When: 다음 요청에는 추가 Advisor를 전달하지 않는다.
        service.chatWithDefaults("일반 질문");

        // Then: 기본 로거만 한 번 더 실행된다. 추가 로거의 총 실행은 여전히 한 번이다.
        assertThat(loggedRequests(logs)).isEqualTo(3);
        verify(additionalLogger, times(1)).adviseCall(any(), any());
        assertThat(MODEL.chat().getRequests()).hasSize(2);
    }

    // 로그 수 확인용 보조 코드. 등록·추가 동작은 위 Service 호출과 검증문에서 읽는다.
    private long loggedRequests(CapturedOutput logs) {
        return logs.getOut().lines()
                .filter(line -> line.contains("SimpleLoggerAdvisor") && line.contains("request: "))
                .count();
    }
}
