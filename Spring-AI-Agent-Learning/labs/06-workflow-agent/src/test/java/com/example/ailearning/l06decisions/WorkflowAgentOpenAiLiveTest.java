package com.example.ailearning.l06decisions;

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
        "spring.ai.openai.chat.timeout=30s"
})
class WorkflowAgentOpenAiLiveTest {
    @Autowired ComparisonService service;

    @Test
    @Timeout(180)
    void 실제_OpenAI로_Workflow와_Agent와_혼합_방식을_비교한다(TestInfo info) {
        for (String mode : java.util.List.of("workflow", "agent", "hybrid")) {
            RunResult result = service.run(mode, "배송비 포함 30000원 이하 음료 선물을 추천해줘.");
            System.out.printf("[%s] %s 결과: %s%n", info.getTestMethod().orElseThrow().getName(), mode, result);
            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.answer()).isNotBlank();
            assertThat(result.modelCalls()).isPositive();
        }
    }
}
