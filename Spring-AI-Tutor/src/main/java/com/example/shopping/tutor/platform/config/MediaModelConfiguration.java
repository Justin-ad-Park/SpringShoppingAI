package com.example.shopping.tutor.platform.config;

import com.example.shopping.tutor.lesson.l14multimodal.AudioModelConfiguration;
import com.example.shopping.tutor.lesson.l14multimodal.ImageModelConfiguration;
import com.example.shopping.tutor.lesson.l14multimodal.ModerationModelConfiguration;

import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** 이미지·음성·moderation API는 채팅 모델과 별도로 설정한다. */
@Component
public class MediaModelConfiguration {
    public final OpenAiImageModel image;
    public final OpenAiAudioTranscriptionModel transcription;
    public final OpenAiAudioSpeechModel speech;
    public final OpenAiModerationModel moderation;

    public MediaModelConfiguration(
            @Value("${tutor.api-key}") String key,
            @Value("${tutor.base-url}") String url,
            @Value("${TUTOR_IMAGE_MODEL:gpt-image-1.5}") String imageModel,
            @Value("${TUTOR_TRANSCRIPTION_MODEL:whisper-1}") String transcriptionModel,
            @Value("${TUTOR_SPEECH_MODEL:tts-1}") String speechModel,
            @Value("${TUTOR_MODERATION_MODEL:omni-moderation-latest}") String moderationModel) {
        String apiKey = key.isBlank() ? "stub-not-a-key" : key;
        var factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(60));
        var rest = RestClient.builder().requestFactory(factory);
        var retry = RetryTemplate.builder().maxAttempts(1).build();
        image = ImageModelConfiguration.create(apiKey, url, imageModel, rest.clone(), retry);
        var audio =
                OpenAiAudioApi.builder()
                        .apiKey(apiKey)
                        .baseUrl(url)
                        .restClientBuilder(rest.clone())
                        .build();
        transcription = AudioModelConfiguration.transcription(audio, transcriptionModel, retry);
        speech = AudioModelConfiguration.speech(audio, speechModel, retry);
        moderation =
                ModerationModelConfiguration.create(
                        apiKey, url, moderationModel, rest.clone(), retry);
    }
}
