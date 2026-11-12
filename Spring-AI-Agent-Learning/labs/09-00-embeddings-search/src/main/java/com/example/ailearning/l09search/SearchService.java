package com.example.ailearning.l09search;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;

@Service
public class SearchService {
    public record SearchHit(String id, String text, String category, Double score) {}
    public record SearchComparison(List<SearchHit> keywordHits, List<SearchHit> vectorHits) {}
    public record EmbeddingVector(int dimensions, float[] values) {}
    private final EmbeddingModel embeddingModel;
    private final SimpleVectorStore policyVectorStore;

    public SearchService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        policyVectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();
        policyVectorStore.add(PolicyDocuments.createDocuments()); // 시작 시 문서를 임베딩해 인메모리 저장소에 적재한다.
    }

    public EmbeddingVector embed(String text) {
        float[] vectorValues = embeddingModel.embed(text);
        return new EmbeddingVector(vectorValues.length, vectorValues);
    }

    public SearchComparison compareSearchResults(String query, int topK, double threshold, String category) {
        if (category != null && !Set.of("shipping", "returns")
                .contains(category))
            throw new IllegalArgumentException("category는 shipping/returns입니다.");
        List<SearchHit> keywordHits = searchByKeyword(query, category);
        List<SearchHit> vectorHits = searchByVector(query, topK, threshold, category);
        return new SearchComparison(keywordHits, vectorHits);
    }

    private List<SearchHit> searchByKeyword(String query, String category) {
        return PolicyDocuments.createDocuments()
                .stream()
                .filter(document -> category == null || category.equals(document
                                .getMetadata()
                                .get("category")))
                .filter(document -> document
                        .getText()
                        .contains(query))
                .map(this::toSearchHit)
                .toList();
    }

    private List<SearchHit> searchByVector(String query, int topK, double threshold, String category) {
        var vectorSearchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(threshold);
        // 허용 목록으로 검사한 메타데이터 값만 필터에 사용한다.
        if (category != null) vectorSearchRequest.filterExpression("category == '" + category + "'");
        return policyVectorStore
                .similaritySearch(vectorSearchRequest.build())
                .stream()
                .map(this::toSearchHit)
                .toList();
    }

    private SearchHit toSearchHit(Document document) {
        return new SearchHit(document.getId(), document.getText(), (String) document
                .getMetadata()
                .get("category"), document.getScore());
    }
}
