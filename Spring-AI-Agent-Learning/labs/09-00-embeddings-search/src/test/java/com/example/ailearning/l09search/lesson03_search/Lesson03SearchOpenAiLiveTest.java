package com.example.ailearning.l09search.lesson03_search;

import com.example.ailearning.l09search.lesson01_vectors.Lesson01EmbeddingService;

import com.example.ailearning.testsupport.output.TestNamePrinter;
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
class Lesson03SearchOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired Lesson03SearchService service;
    @Autowired Lesson01EmbeddingService embeddingService;

    @Test
    @Timeout(180)
    void 실제_OpenAI로_텍스트를_임베딩하고_문서를_검색한다() {
        // 숫자 배열을 출력하려고 임베딩을 한 번 요청한다.
        Lesson01EmbeddingService.EmbeddingVector vector = embeddingService.embed("배송비는 얼마인가요?");

        System.out.printf("벡터 차원: %d, 앞 8개 값: %s%n", vector.dimensions(),
                java.util.Arrays.toString(java.util.Arrays.copyOf(vector.values(), Math.min(8, vector.dimensions()))));
        // 검색은 위 vector를 재사용하지 않고, 저장소 내부에서 같은 질문을 다시 임베딩한다.
        Lesson03SearchService.SearchComparison result = service.compareSearchResults("배송비는 얼마인가요?", 2, 0.0, null);
        System.out.printf("문자열 검색과 벡터 검색: %s%n", result);
        // 연결·결과 존재를 검사한다. 관련 문서를 찾았는지는 docs/lessons/09-embeddings-search/03-search.md의 기준으로 비교한다.
        assertThat(vector.dimensions()).isPositive();
        assertThat(result.vectorHits()).isNotEmpty();
    }
}
