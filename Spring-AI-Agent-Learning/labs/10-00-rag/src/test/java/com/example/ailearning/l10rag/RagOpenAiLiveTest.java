package com.example.ailearning.l10rag;

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
class RagOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired RagService service;

    @Test
    @Timeout(180)
    void 실제_OpenAI의_답변과_출처_목록을_확인한다() {
        RagService.Reply result = service.answer("미개봉 상품의 반품 기간은 며칠인가요?");
        System.out.printf("답변과 출처: %s%n", result);
        // 답변·출처 존재를 검사한다. 주장과 원문의 일치는 docs/lessons/10-rag/03-evidence.md의 기준으로 별도 확인한다.
        assertThat(result.status()).isEqualTo("ANSWERED");
        assertThat(result.answer()).isNotBlank();
        assertThat(result.sources()).isNotEmpty();
    }
}
