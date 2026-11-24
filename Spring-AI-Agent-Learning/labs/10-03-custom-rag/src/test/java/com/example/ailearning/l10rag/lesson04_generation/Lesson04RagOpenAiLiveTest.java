package com.example.ailearning.l10rag.lesson04_generation;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import com.example.ailearning.l10rag.lesson03_indexing.Lesson03PolicySearch;
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
class Lesson04RagOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired Lesson04RagService service;
    @Autowired Lesson03PolicySearch policySearch;

    @Test
    @Timeout(180)
    void 실제_OpenAI의_답변과_출처_목록을_확인한다() {
        String question = "미개봉 상품의 반품 기간은 며칠인가요?";
        System.out.println("고객 질문: " + question);
        // 검색 원문을 따로 출력하므로 answer() 내부 검색 외에 질문 임베딩이 한 번 더 발생한다.
        System.out.println("비교를 위해 별도로 검색한 정책 항목: " + policySearch.searchPolicies(question));
        Lesson04RagService.Reply result = service.answer(question);
        System.out.printf("답변과 출처: %s%n", result);
        // 답변·출처 존재를 검사한다. 주장과 원문의 일치는 docs/lessons/10-rag/05-evidence.md의 기준으로 별도 확인한다.
        assertThat(result.status()).isEqualTo("ANSWERED");
        assertThat(result.answer()).isNotBlank();
        assertThat(result.sources()).isNotEmpty();
    }

    @Test
    @Timeout(180)
    void 실제_OpenAI가_여러_정책_중_개봉_반품_문의에_답한다() {
        String question = "상품을 개봉했는데 단순 변심으로 반품할 수 있나요?";
        System.out.println("고객 질문: " + question);
        // 표시를 위한 검색과 answer() 내부 검색은 별도 실행이다.
        System.out.println("비교를 위해 별도로 검색한 정책 항목: " + policySearch.searchPolicies(question));
        Lesson04RagService.Reply result = service.answer(question);
        System.out.println("질문에 대한 답변: " + result.answer());
        System.out.println("답변이 인용한 정책 원문: " + result.sources());
        assertThat(result.status()).isEqualTo("ANSWERED");
        assertThat(result.answer()).isNotBlank();
        assertThat(result.sources()).extracting(source -> source.id()).contains("return-opened");
    }
}
