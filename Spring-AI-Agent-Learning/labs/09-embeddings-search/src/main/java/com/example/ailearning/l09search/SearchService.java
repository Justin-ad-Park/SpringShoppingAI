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
    public record Hit(String id, String text, String category, Double score) {}
    public record Comparison(List<Hit> keywordHits, List<Hit> vectorHits) {}
    public record Vector(int dimensions, float[] values) {}
    private final EmbeddingModel embedding;
    private final SimpleVectorStore store;

    public SearchService(EmbeddingModel embedding) {
        this.embedding = embedding;
        store = SimpleVectorStore.builder(embedding)
                .build();
        store.add(PolicyDocuments.all()); // 시작 시 문서를 임베딩해 인메모리 저장소에 적재한다.
    }

    public Vector embed(String text) {
        float[] values = embedding.embed(text);
        return new Vector(values.length, values);
    }

    public Comparison search(String query, int topK, double threshold, String category) {
        if (category != null && !Set.of("shipping", "returns")
                .contains(category))
            throw new IllegalArgumentException("category는 shipping/returns입니다.");
        List<Hit> keyword = PolicyDocuments.all()
                .stream()
                .filter(doc -> category == null || category.equals(doc
                                .getMetadata()
                                .get("category")))
                .filter(doc -> doc
                        .getText()
                        .contains(query))
                .map(this::view)
                .toList();
        var request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(threshold);
        // 허용 목록으로 검사한 메타데이터 값만 필터에 사용한다.
        if (category != null) request.filterExpression("category == '" + category + "'");
        List<Hit> semantic = store
                .similaritySearch(request.build())
                .stream()
                .map(this::view)
                .toList();
        return new Comparison(keyword, semantic);
    }

    private Hit view(Document doc) {
        return new Hit(doc.getId(), doc.getText(), (String) doc
                .getMetadata()
                .get("category"), doc.getScore());
    }
}
