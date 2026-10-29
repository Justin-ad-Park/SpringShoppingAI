package com.example.shopping.tutor.lesson.l09rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.*;

import java.util.*;

/** 검색 대상 몰·버전은 프롬프트가 아닌 저장소 필터로 제한한다. */
public final class PolicyVectorIndex {
    private PolicyVectorIndex() {}

    public static SimpleVectorStore create(
            EmbeddingModel embeddingModel, List<Document> documents) {
        var store = SimpleVectorStore.builder(embeddingModel).build();
        store.add(documents);
        return store;
    }

    public static SearchRequest request(String query, int topK) {
        return SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.35)
                .filterExpression("mallId == 'TUTOR' && current == true")
                .build();
    }
}
