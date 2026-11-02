package com.example.shopping.tutor.lesson.l00setup;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.openai.*;
import java.time.Duration;

/** 2.0은 OpenAI SDK를 사용한다. 옵션으로 연결·출력·재시도 한도를 명시한다. */
public final class ChatModelConfiguration {
    private ChatModelConfiguration() {}

    public static OpenAiChatModel create(
            String key, String baseUrl, String modelName, ObservationRegistry observation) {
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder().apiKey(key).baseUrl(baseUrl)
                        .model(modelName).maxTokens(800).maxRetries(0)
                        .timeout(Duration.ofSeconds(45)).build())
                .observationRegistry(observation)
                .build();
    }
}
