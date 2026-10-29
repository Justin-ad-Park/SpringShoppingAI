package com.example.shopping.tutor.lesson.l07vector;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;

/** 임베딩 모델이 바뀌어 차원이 달라지면 저장된 문서는 재색인해야 한다. */
public final class EmbeddingModelConfiguration {
    private EmbeddingModelConfiguration() {}

    public static OpenAiEmbeddingModel create(
            OpenAiApi api, String modelName, ObservationRegistry observation) {
        return new OpenAiEmbeddingModel(
                api,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder().model(modelName).build(),
                RetryTemplate.builder().maxAttempts(1).build(),
                observation);
    }
}
