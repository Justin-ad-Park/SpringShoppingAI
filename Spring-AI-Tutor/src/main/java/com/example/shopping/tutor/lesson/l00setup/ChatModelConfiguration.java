package com.example.shopping.tutor.lesson.l00setup;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;

/** 모델명·최대 출력 토큰을 정한다. API 키와 네트워크 설정은 platform에서 주입한다. */
public final class ChatModelConfiguration {
    private ChatModelConfiguration() {}

    public static OpenAiChatModel create(
            OpenAiApi api, String modelName, ObservationRegistry observation) {
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(modelName).maxTokens(800).build())
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).build())
                .observationRegistry(observation)
                .build();
    }
}
