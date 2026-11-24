package com.example.ailearning.l09search.lesson02_store;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

// 실제 모델 API를 호출하며 루트 .env가 필요합니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.profiles.active=basic-search"})
class Lesson02BasicVectorSearchOpenAiLiveTest {
    @Autowired Lesson02BasicVectorSearchService basicVectorSearchService;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) { TestNamePrinter.printTestName(testInfo); }

    @Test
    @Timeout(180)
    void 실제_임베딩으로_문서를_적재하고_검색한다() {
        var documents = basicVectorSearchService.search("배송비가 얼마인가요?", 1);
        System.out.printf("검색 문서: %s%n", documents);
        assertThat(documents).hasSize(1);
    }
}
