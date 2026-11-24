package com.example.ailearning.l09search.lesson01_vectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

/** 09-1: 텍스트 하나의 임베딩만 요청한다. 문서를 적재하지 않는다. */
@Service
public class Lesson01EmbeddingService {
    public record EmbeddingVector(int dimensions, float[] values) {}
    private final EmbeddingModel embeddingModel;

    public Lesson01EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public EmbeddingVector embed(String text) {
        float[] vectorValues = embeddingModel.embed(text);
        return new EmbeddingVector(vectorValues.length, vectorValues);
    }
}
