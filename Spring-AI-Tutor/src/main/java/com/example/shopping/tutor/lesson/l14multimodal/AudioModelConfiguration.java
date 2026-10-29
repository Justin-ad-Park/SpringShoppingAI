package com.example.shopping.tutor.lesson.l14multimodal;

import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.retry.support.RetryTemplate;

/** 같은 오디오 API를 사용해도 전사와 음성 합성은 서로 다른 모델·옵션이다. */
public final class AudioModelConfiguration {
    private AudioModelConfiguration() {}

    public static OpenAiAudioTranscriptionModel transcription(
            OpenAiAudioApi audioApi, String modelName, RetryTemplate retry) {
        return new OpenAiAudioTranscriptionModel(
                audioApi,
                OpenAiAudioTranscriptionOptions.builder()
                        .model(modelName)
                        .language("ko")
                        .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.JSON)
                        .build(),
                retry);
    }

    public static OpenAiAudioSpeechModel speech(
            OpenAiAudioApi audioApi, String modelName, RetryTemplate retry) {
        return new OpenAiAudioSpeechModel(
                audioApi,
                OpenAiAudioSpeechOptions.builder().model(modelName).voice("alloy").build(),
                retry);
    }
}
