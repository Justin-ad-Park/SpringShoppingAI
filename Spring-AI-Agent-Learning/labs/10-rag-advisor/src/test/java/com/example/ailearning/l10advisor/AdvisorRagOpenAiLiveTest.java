package com.example.ailearning.l10advisor;

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
        "spring.ai.openai.chat.timeout=30s",
        "spring.ai.openai.embedding.timeout=30s"
})
class AdvisorRagOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired AdvisorRagService service;

    @Test
    @Timeout(180)
    void 실제_OpenAI에_RAG_Advisor가_검색한_문서를_전달한다() {
        String answer = service.answer("미개봉 상품의 반품 기간은 며칠인가요?");
        System.out.printf("RAG Advisor 답변: %s%n", answer);
        assertThat(answer).isNotBlank();
    }
}
