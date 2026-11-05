package com.example.ailearning.l08advisors;

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
class AdvisorOpenAiLiveTest {
    @Autowired AdvisorChatService service;

    @Test
    @Timeout(180)
    void 실제_OpenAI_요청에서_Advisor_순서와_메모리를_확인한다(TestInfo info) {
        AdvisorChatService.Reply result = service.chat("live-user", "advisor-demo", "도구로 허브티 가격을 확인해줘.", true);
        System.out.printf("[%s] 답변과_Advisor_순서: %s%n", info
                        .getTestMethod()
                        .orElseThrow()
                        .getName(), result);
        assertThat(result.answer()).isNotBlank();
        assertThat(result.steps()).startsWith("A:before").endsWith("A:after");
        assertThat(result.storedMessages()).isEqualTo(2);
    }
}
