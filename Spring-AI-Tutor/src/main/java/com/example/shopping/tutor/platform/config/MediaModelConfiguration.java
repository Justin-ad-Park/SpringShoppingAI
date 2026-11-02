package com.example.shopping.tutor.platform.config;

import com.example.shopping.tutor.lesson.l14multimodal.AudioModelConfiguration;
import com.example.shopping.tutor.lesson.l14multimodal.ImageModelConfiguration;
import com.example.shopping.tutor.lesson.l14multimodal.ModerationModelConfiguration;

import org.springframework.ai.openai.*;

import org.springframework.beans.factory.annotation.Value;


import org.springframework.stereotype.Component;




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
        String baseUrl = AiModelConfiguration.sdkBaseUrl(url);
        image = ImageModelConfiguration.create(apiKey, baseUrl, imageModel);
        transcription = AudioModelConfiguration.transcription(apiKey, baseUrl, transcriptionModel);
        speech = AudioModelConfiguration.speech(apiKey, baseUrl, speechModel);
        moderation = ModerationModelConfiguration.create(apiKey, baseUrl, moderationModel);
    }
}
