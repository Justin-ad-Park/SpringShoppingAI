package com.example.shopping.tutor.lesson.l07vector;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.*;
import java.time.Duration;

/** 임베딩 모델이 바뀌어 차원이 달라지면 저장된 문서는 재색인해야 한다. */
public final class EmbeddingModelConfiguration {
    private EmbeddingModelConfiguration() {}

    public static OpenAiEmbeddingModel create(
            String key, String baseUrl, String modelName, ObservationRegistry observation) {
        return OpenAiEmbeddingModel.builder()
                .metadataMode(MetadataMode.EMBED)
                .options(OpenAiEmbeddingOptions.builder().apiKey(key).baseUrl(baseUrl)
                        .model(modelName).maxRetries(0).timeout(Duration.ofSeconds(45)).build())
                .observationRegistry(observation).build();
    }
}
