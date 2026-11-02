package com.example.shopping.tutor.lesson.l14multimodal;

import org.springframework.ai.openai.*;
import java.time.Duration;

/** 이미지 모델은 채팅 모델과 별도의 API와 옵션을 사용한다. */
public final class ImageModelConfiguration {
    private ImageModelConfiguration() {}

    public static OpenAiImageModel create(String key, String url, String modelName) {
        return OpenAiImageModel.builder()
                .options(OpenAiImageOptions.builder().apiKey(key).baseUrl(url).model(modelName)
                        .n(1).width(1024).height(1024).maxRetries(0)
                        .timeout(Duration.ofSeconds(60)).build()).build();
    }
}
