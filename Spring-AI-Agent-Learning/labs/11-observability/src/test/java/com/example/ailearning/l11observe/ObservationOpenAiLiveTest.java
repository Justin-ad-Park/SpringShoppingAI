package com.example.ailearning.l11observe;

import com.example.ailearning.testsupport.TestNamePrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 OpenAI를 호출합니다. 저장소 루트 .env를 사용하며 API 사용량이 발생합니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.ai.openai.base-url=https://api.openai.com/v1",
        "spring.ai.openai.chat.timeout=30s"
})
class ObservationOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired ObservedChatService service;
    @Autowired io.micrometer.core.instrument.MeterRegistry registry;

    @Test
    @Timeout(180)
    void 실제_OpenAI_응답과_호출_지표를_함께_출력한다() {
        // Given: 호출 전 모델 요청 횟수 조회
        long before = registry
                .find("gen_ai.client.operation")
                .timers()
                .stream()
                .mapToLong(timer -> timer.count())
                .sum();

        // When: 실제 OpenAI로 질문 전달
        String answer = service.ask("Spring의 의존성 주입을 한 문장으로 설명해줘.");

        // Then: 응답·호출 후 지표를 출력하고 호출 횟수 증가 검증

        System.out.printf("답변: %s%n", answer);
        for (var meter : registry.getMeters()) {
            if (meter
                    .getId()
                    .getName()
                    .startsWith("gen_ai.client.")) {
                for (var value : meter.measure()) {
                    System.out.printf("%s / %s: %s%n", meter
                                    .getId()
                                    .getName(), value.getStatistic(), value.getValue());
                }
            }
        }
        long after = registry
                .find("gen_ai.client.operation")
                .timers()
                .stream()
                .mapToLong(timer -> timer.count())
                .sum();
        assertThat(answer).isNotBlank();
        assertThat(after).isGreaterThan(before);
    }
}
