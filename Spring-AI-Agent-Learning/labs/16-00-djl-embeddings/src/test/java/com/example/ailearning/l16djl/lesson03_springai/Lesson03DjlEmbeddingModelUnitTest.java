package com.example.ailearning.l16djl.lesson03_springai;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import com.example.ailearning.l16djl.lesson02_vectors.Lesson02DjlEmbeddingService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import java.util.List;
import static org.mockito.Mockito.*;


class Lesson03DjlEmbeddingModelUnitTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 저장할_문서와_검색_질문을_각각의_임베딩_메서드로_연결한다() {
        // Given: 모델 실행 없이 문서·질문이 연결되는 경로를 고정 벡터로 확인한다.
        var embeddingService = mock(Lesson02DjlEmbeddingService.class);
        float[] shippingVector = new float[384];
        shippingVector[0] = 1;
        float[] returnVector = new float[384];
        returnVector[1] = 1;
        when(embeddingService.embedDocument("배송 정책")).thenReturn(shippingVector);
        when(embeddingService.embedDocument("반품 정책")).thenReturn(returnVector);
        when(embeddingService.embedQuery("운송 요금")).thenReturn(shippingVector);
        var vectorStore = SimpleVectorStore.builder(new Lesson03DjlEmbeddingModel(embeddingService))
                .build();

        // When: SimpleVectorStore의 문서 일괄 적재와 문자열 질문 검색을 모두 실행한다.
        vectorStore.add(List.of(new Document("배송 정책"), new Document("반품 정책")));
        var hits = vectorStore.similaritySearch(SearchRequest.builder()
                .query("운송 요금")
                .topK(1)
                .build());

        // Then: 실제로 거친 임베딩 메서드와 검색 결과를 확인한다.
        System.out.println("고정 벡터 검색 결과: " + hits);
        assertThat(hits).hasSize(1);
        assertThat(hits.getFirst().getText()).isEqualTo("배송 정책");
        verify(embeddingService).embedDocument("배송 정책");
        verify(embeddingService).embedDocument("반품 정책");
        verify(embeddingService).embedQuery("운송 요금");
    }
}
