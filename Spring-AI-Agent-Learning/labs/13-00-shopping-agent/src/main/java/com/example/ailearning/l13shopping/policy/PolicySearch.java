package com.example.ailearning.l13shopping.policy;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.*;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;

@Service
public class PolicySearch {
    private final SimpleVectorStore store;
    public PolicySearch(EmbeddingModel embedding) throws IOException {
        store = SimpleVectorStore.builder(embedding)
                .build();
        store.add(PolicyLoader.loadDocuments());
    }
    public List<PolicySearchResult> searchPolicies(String question) {
        return search(SearchRequest.builder()
                .query(question)
                .topK(2)
                .similarityThreshold(0.65)
                .build());
    }

    public List<PolicySearchResult> searchReturnPolicies(String question) {
        // 반품 전용 도구에는 개봉·미개봉 정책을 모두 근거로 제공한다.
        return search(SearchRequest.builder()
                .query(question)
                .filterExpression("source in ['policies.md#return-sealed', 'policies.md#return-opened']")
                .similarityThreshold(SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL)
                .topK(2)
                .build());
    }

    private List<PolicySearchResult> search(SearchRequest request) {
        return store
                .similaritySearch(request)
                .stream()
                .map(PolicySearchResult::fromDocument)
                .toList();
    }
}
