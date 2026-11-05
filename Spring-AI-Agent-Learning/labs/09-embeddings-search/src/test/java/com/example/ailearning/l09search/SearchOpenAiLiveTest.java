package com.example.ailearning.l09search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 OpenAI를 호출합니다. 저장소 루트 .env를 사용하며 API 사용량이 발생합니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.ai.openai.base-url=https://api.openai.com/v1",
        "spring.ai.openai.embedding.timeout=30s"
})
class SearchOpenAiLiveTest {
    @Autowired SearchService service;

    @Test
    @Timeout(180)
    void 실제_OpenAI로_텍스트를_임베딩하고_문서를_검색한다(TestInfo info) {
        SearchService.Vector vector = service.embed("배송비는 얼마인가요?");
        String name = info
                .getTestMethod()
                .orElseThrow()
                .getName();
        System.out.printf("[%s] 벡터 차원: %d, 앞 8개 값: %s%n", name, vector.dimensions(),
                java.util.Arrays.toString(java.util.Arrays.copyOf(vector.values(), Math.min(8, vector.dimensions()))));
        SearchService.Comparison result = service.search("배송비는 얼마인가요?", 2, 0.0, null);
        System.out.printf("[%s] 문자열 검색과 벡터 검색: %s%n", name, result);
        assertThat(vector.dimensions()).isPositive();
        assertThat(result.vectorHits()).isNotEmpty();
    }
}
