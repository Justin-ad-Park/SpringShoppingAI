package com.example.ailearning.l13shopping;

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
        "learning.limits.timeout-ms=90000",
        "spring.ai.openai.chat.timeout=30s",
        "spring.ai.openai.embedding.timeout=30s"
})
class ShoppingAgentOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired CounselService service;

    @Test
    @Timeout(180)
    void 실제_OpenAI가_도구를_선택하고_후속_대화의_조건을_사용한다() {
        // Given: 예산이 있는 첫 상담을 수행해 이전 대화 준비
        CounselService.Reply first = service.chat("live-user", "counsel-demo", "배송비 포함 30000원 이하 음료를 추천해줘.");

        System.out.printf("첫 상담과 도구 기록: %s%n", first);
        assertThat(first.status()).isEqualTo("VERIFIED");
        assertThat(first.offers()).isNotEmpty();

        // When: 같은 대화에서 예산 변경 요청
        CounselService.Reply next = service.chat("live-user", "counsel-demo", "예산을 40000원으로 바꿔줘.");

        // Then: 후속 응답과 변경된 예산 검증
        System.out.printf("후속 상담과 도구 기록: %s%n", next);
        assertThat(next.status()).isEqualTo("VERIFIED");
        assertThat(next.criteria().totalBudget()).isEqualTo(40000);
    }
}
