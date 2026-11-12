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
        "spring.ai.openai.timeout=30s"
})
class AdvisorRagOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired AdvisorRagService service;

    @Test
    @Timeout(180)
    void 실제_OpenAI로_RAG_Advisor의_답변을_받는다() {
        String answer = service.answer("미개봉 상품의 반품 기간은 며칠인가요?");
        System.out.printf("RAG Advisor 답변: %s%n", answer);
        // 이 Live 테스트는 응답 수신만 확인합니다. 검색 문서의 실제 전달은
        // AdvisorRagLocalModelTest의 모델 요청 메시지 검사에서 확인합니다.
        assertThat(answer).isNotBlank();
    }
}
