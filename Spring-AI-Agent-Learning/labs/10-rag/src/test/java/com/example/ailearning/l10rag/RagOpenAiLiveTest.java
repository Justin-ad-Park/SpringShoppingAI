package com.example.ailearning.l10rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 OpenAI를 호출합니다. 저장소 루트 .env를 사용하며 API 사용량이 발생합니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.ai.openai.base-url=https://api.openai.com/v1",
        "spring.ai.openai.chat.timeout=30s",
        "spring.ai.openai.embedding.timeout=30s"
})
class RagOpenAiLiveTest {
    @Autowired RagService service;

    @Test
    @Timeout(180)
    void 실제_OpenAI로_검색한_정책을_근거로_답한다(TestInfo info) {
        RagService.Reply result = service.answer("미개봉 상품의 반품 기간은 며칠인가요?");
        System.out.printf("[%s] 답변과 출처: %s%n", info
                        .getTestMethod()
                        .orElseThrow()
                        .getName(), result);
        assertThat(result.status()).isEqualTo("ANSWERED");
        assertThat(result.answer()).isNotBlank();
        assertThat(result.sources()).isNotEmpty();
    }
}
