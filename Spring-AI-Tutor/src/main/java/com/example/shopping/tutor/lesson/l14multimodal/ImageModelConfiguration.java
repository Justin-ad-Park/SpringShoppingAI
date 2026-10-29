package com.example.shopping.tutor.lesson.l14multimodal;

import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

/** 이미지 모델은 채팅 모델과 별도의 API와 옵션을 사용한다. */
public final class ImageModelConfiguration {
    private ImageModelConfiguration() {}

    public static OpenAiImageModel create(
            String key,
            String url,
            String modelName,
            RestClient.Builder restClient,
            RetryTemplate retry) {
        return new OpenAiImageModel(
                OpenAiImageApi.builder()
                        .apiKey(key)
                        .baseUrl(url)
                        .restClientBuilder(restClient)
                        .build(),
                OpenAiImageOptions.builder().model(modelName).N(1).width(1024).height(1024).build(),
                retry);
    }
}
