package com.example.ailearning.l09search.lesson01_vectors;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

// 실제 모델 API를 호출하며 루트 .env가 필요합니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.profiles.active=embedding-only"})
class Lesson01EmbeddingOnlyOpenAiLiveTest {
    @Autowired Lesson01EmbeddingService embeddingService;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) { TestNamePrinter.printTestName(testInfo); }

    @Test
    @Timeout(180)
    void 실제_모델로_문서_적재_없이_텍스트를_임베딩한다() {
        var vector = embeddingService.embed("배송 요금");
        System.out.printf("벡터 차원: %d%n", vector.dimensions());
        assertThat(vector.dimensions()).isPositive();
    }
}
