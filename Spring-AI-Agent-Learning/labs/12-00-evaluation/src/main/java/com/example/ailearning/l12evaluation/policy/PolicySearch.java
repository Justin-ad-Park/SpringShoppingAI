package com.example.ailearning.l12evaluation.policy;

import java.io.IOException;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

/** 시작할 때 정책 문서를 적재하고, 질문과 가까운 정책을 검색한다. */
@Service
public class PolicySearch {
    private final SimpleVectorStore policyVectorStore;

    public PolicySearch(EmbeddingModel embeddingModel) throws IOException {
        policyVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        policyVectorStore.add(PolicyLoader.loadDocuments());
    }

    public List<PolicySearchResult> searchPolicies(String question) {
        return policyVectorStore
                .similaritySearch(SearchRequest.builder()
                        .query(question)
                        .topK(2)
                        .similarityThreshold(0.65)
                        .build())
                .stream()
                .map(PolicySearchResult::fromDocument)
                .toList();
    }
}
