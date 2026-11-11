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
        return store
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
