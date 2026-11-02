package com.example.shopping.tutor.lesson.l14multimodal;

import org.springframework.ai.openai.*;
import com.openai.models.audio.AudioResponseFormat;
import java.time.Duration;

/** 같은 오디오 API를 사용해도 전사와 음성 합성은 서로 다른 모델·옵션이다. */
public final class AudioModelConfiguration {
    private AudioModelConfiguration() {}

    public static OpenAiAudioTranscriptionModel transcription(String key, String url, String modelName) {
        return OpenAiAudioTranscriptionModel.builder()
                .options(OpenAiAudioTranscriptionOptions.builder().apiKey(key).baseUrl(url)
                        .model(modelName).language("ko").responseFormat(AudioResponseFormat.JSON)
                        .maxRetries(0).timeout(Duration.ofSeconds(60)).build()).build();
    }

    public static OpenAiAudioSpeechModel speech(String key, String url, String modelName) {
        return OpenAiAudioSpeechModel.builder()
                .options(OpenAiAudioSpeechOptions.builder().apiKey(key).baseUrl(url)
                        .model(modelName).voice("alloy").maxRetries(0)
                        .timeout(Duration.ofSeconds(60)).build()).build();
    }
}
