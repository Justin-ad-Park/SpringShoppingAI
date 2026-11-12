package com.example.ailearning.l08advisors;

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
        "spring.ai.openai.timeout=30s"
})
class AdvisorOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired AdvisorChatService service;

    @Test
    @Timeout(180)
    void 실제_OpenAI_호출의_Advisor_전후_기록과_저장_개수를_확인한다() {
        AdvisorChatService.Reply result = service.chat("live-user", "advisor-demo", "허브티 가격은 얼마인가요?", true);
        System.out.printf("답변과_Advisor_순서: %s%n", result);
        assertThat(result.answer()).isNotBlank();
        assertThat(result.steps()).startsWith("A:before").endsWith("A:after");
        // 반복 횟수는 고정하지 않고, 안쪽 Advisor의 전후 기록도 확인합니다.
        assertThat(result.steps()).containsSubsequence("A:before", "B:before", "B:after", "A:after");
        assertThat(result.storedMessages()).isEqualTo(2);
    }
}
