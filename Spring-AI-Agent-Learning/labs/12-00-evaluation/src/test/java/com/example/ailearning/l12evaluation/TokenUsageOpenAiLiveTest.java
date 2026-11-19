package com.example.ailearning.l12evaluation;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

// 실제 모델 API를 호출하며 루트 .env가 필요합니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.profiles.active=usage-only", "spring.ai.model.embedding=none"})
class TokenUsageOpenAiLiveTest {
    @Autowired com.example.ailearning.l12evaluation.usage.TokenUsageService tokenUsageService;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) { TestNamePrinter.printTestName(testInfo); }

    @Test
    @Timeout(180)
    void 실제_채팅_응답의_답변과_사용량을_읽는다() {
        var reply = tokenUsageService.ask("짧게 인사해 주세요.");
        System.out.printf("답변과 사용량: %s%n", reply);
        assertThat(reply.answer()).isNotBlank();
        assertThat(reply.inputTokens()).isNotNull();
        assertThat(reply.outputTokens()).isNotNull();
    }
}
