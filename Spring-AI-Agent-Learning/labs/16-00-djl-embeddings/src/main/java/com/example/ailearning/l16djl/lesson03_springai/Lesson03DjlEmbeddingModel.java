package com.example.ailearning.l16djl.lesson03_springai;

import com.example.ailearning.l16djl.lesson02_vectors.Lesson02DjlEmbeddingService;
import java.util.List;
import java.util.ArrayList;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 16-3: DJL 추론을 Spring AI의 EmbeddingModel로 연결하는 이 실습의 어댑터. */
@Profile("!embedding-only")
@Component
public class Lesson03DjlEmbeddingModel implements EmbeddingModel {
    private final Lesson02DjlEmbeddingService embeddingService;

    public Lesson03DjlEmbeddingModel(Lesson02DjlEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    // 이 실습은 384차원의 multilingual-e5-small-ko-v2를 사용한다. 차원 조회용 추론은 생략한다.
    @Override
    public int dimensions() {
        return 384;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        var texts = request.getInstructions();
        var embeddings = new ArrayList<Embedding>();
        for (int index = 0; index < texts.size(); index++) {
            float[] vector = embeddingService.embedQuery(texts.get(index));
            embeddings.add(new Embedding(vector, index));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embeddingService.embedDocument(document.getText());
    }

    // SimpleVectorStore는 문서를 일괄 전달한다. 각 문서를 문서용 임베딩으로 연결한다.
    @Override
    public List<float[]> embed(List<Document> documents, EmbeddingOptions options,
            BatchingStrategy batchingStrategy) {
        return documents.stream()
                .map(this::embed)
                .toList();
    }
}
