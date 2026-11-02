package com.example.shopping.tutor.lesson.l14multimodal;

import org.springframework.ai.openai.*;
import java.time.Duration;

/** 콘텐츠 판정 설정. 쇼핑 권한이나 상품 사실을 검증하는 설정은 아니다. */
public final class ModerationModelConfiguration {
    private ModerationModelConfiguration() {}

    public static OpenAiModerationModel create(String key, String url, String modelName) {
        return OpenAiModerationModel.builder()
                .options(OpenAiModerationOptions.builder().apiKey(key).baseUrl(url).model(modelName)
                        .maxRetries(0).timeout(Duration.ofSeconds(60)).build()).build();
    }
}
