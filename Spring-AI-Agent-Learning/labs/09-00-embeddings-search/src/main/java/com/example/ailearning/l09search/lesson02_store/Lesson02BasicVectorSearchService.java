package com.example.ailearning.l09search.lesson02_store;

import com.example.ailearning.l09search.PolicyDocuments;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** 09-2: 문서 적재와 query·topK 검색만 연결한다. */
@Service
@Profile("basic-search")
public class Lesson02BasicVectorSearchService {
    private final SimpleVectorStore vectorStore;

    public Lesson02BasicVectorSearchService(EmbeddingModel embeddingModel) {
        vectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();
        vectorStore.add(PolicyDocuments.createDocuments());
    }

    public List<Document> search(String question, int topK) {
        return vectorStore
                .similaritySearch(SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .build());
    }
}
