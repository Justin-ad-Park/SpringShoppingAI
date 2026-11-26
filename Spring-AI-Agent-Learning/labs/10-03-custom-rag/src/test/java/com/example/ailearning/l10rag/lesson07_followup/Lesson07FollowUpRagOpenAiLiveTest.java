package com.example.ailearning.l10rag.lesson07_followup;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

/** 실제 OpenAI를 사용한다. 저장소 루트 .env가 필요하며 API 사용량이 발생한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.ai.openai.timeout=30s")
class Lesson07FollowUpRagOpenAiLiveTest {
    @Autowired Lesson07FollowUpRagService service;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) { TestNamePrinter.printTestName(testInfo); }

    @Test
    @Timeout(180)
    void 실제_OpenAI의_처리_결과를_확인한다() {

        var result = service.answer("미개봉 상품의 반품 기간은?", "수령 후 7일 이내입니다.", "그럼 개봉했으면?");
        System.out.println("변환한 검색 질문과 실제 답변: " + result);
        assertThat(result.searchQuestion()).isNotBlank();
        assertThat(result.answer().status()).isEqualTo("ANSWERED");
        assertThat(result.answer().sources()).extracting(source -> source.id()).contains("return-opened");
    }
}
