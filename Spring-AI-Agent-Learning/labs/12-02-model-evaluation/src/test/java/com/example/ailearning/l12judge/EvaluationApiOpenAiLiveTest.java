package com.example.ailearning.l12judge;

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
        "spring.ai.openai.chat.timeout=30s"
})
class EvaluationApiOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired AnswerEvaluator service;

    @Test
    @Timeout(180)
    void 실제_OpenAI_평가_모델에_질문과_근거와_답변을_전달한다() {
        boolean relevance = service.relevance("미개봉 반품 기간은?", "미개봉 상품은 수령 후 7일 이내 반품할 수 있습니다.", "수령 후 7일 이내입니다.");
        boolean facts = service.facts("미개봉 상품은 수령 후 7일 이내 반품할 수 있습니다.", "미개봉 상품의 반품 기간은 7일입니다.");
        System.out.printf("관련성: %s, 근거 일치: %s%n", relevance, facts);
        assertThat(relevance).isTrue();
        assertThat(facts).isTrue();
    }
}
