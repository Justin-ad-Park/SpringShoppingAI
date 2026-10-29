package com.example.shopping.tutor.lesson.l14multimodal;

import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiModerationApi;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

/** 콘텐츠 판정의 모델 설정. 쇼핑 권한이나 상품 사실을 검증하는 설정은 아니다. */
public final class ModerationModelConfiguration {
    private ModerationModelConfiguration() {}

    public static OpenAiModerationModel create(
            String key,
            String url,
            String modelName,
            RestClient.Builder restClient,
            RetryTemplate retry) {
        return new OpenAiModerationModel(
                        OpenAiModerationApi.builder()
                                .apiKey(key)
                                .baseUrl(url)
                                .restClientBuilder(restClient)
                                .build(),
                        retry)
                .withDefaultOptions(OpenAiModerationOptions.builder().model(modelName).build());
    }
}
