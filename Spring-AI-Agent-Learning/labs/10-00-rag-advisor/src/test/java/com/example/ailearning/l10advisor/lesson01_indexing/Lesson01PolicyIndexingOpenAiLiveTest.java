package com.example.ailearning.l10advisor.lesson01_indexing;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

// 실제 모델 API를 호출하며 루트 .env가 필요합니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.profiles.active=indexing-only"})
class Lesson01PolicyIndexingOpenAiLiveTest {
    @Autowired org.springframework.ai.vectorstore.SimpleVectorStore vectorStore;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) { TestNamePrinter.printTestName(testInfo); }

    @Test
    @Timeout(180)
    void 실제_임베딩으로_표준_적재_결과를_검색한다() {
        var documents = vectorStore.similaritySearch("미개봉 반품 기간");
        System.out.printf("검색 문서: %s%n", documents);
        assertThat(documents).isNotEmpty();
    }
}
