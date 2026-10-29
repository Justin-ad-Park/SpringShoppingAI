package com.example.shopping.tutor.support;

import java.time.Duration;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** 이미지·음성·moderation API는 채팅 모델과 별도로 설정한다. */
@Component
public class MediaModels {
    public final OpenAiImageModel image;
    public final OpenAiAudioTranscriptionModel transcription;
    public final OpenAiAudioSpeechModel speech;
    public final OpenAiModerationModel moderation;
    public MediaModels(@Value("${tutor.api-key}") String key,@Value("${tutor.base-url}") String url,
            @Value("${TUTOR_IMAGE_MODEL:gpt-image-1.5}") String imageModel,
            @Value("${TUTOR_TRANSCRIPTION_MODEL:whisper-1}") String transcriptionModel,
            @Value("${TUTOR_SPEECH_MODEL:tts-1}") String speechModel,
            @Value("${TUTOR_MODERATION_MODEL:omni-moderation-latest}") String moderationModel) {
        String apiKey=key.isBlank()?"stub-not-a-key":key;
        var factory=new JdkClientHttpRequestFactory();factory.setReadTimeout(Duration.ofSeconds(60));
        var rest=RestClient.builder().requestFactory(factory);
        var retry=RetryTemplate.builder().maxAttempts(1).build();
        image=new OpenAiImageModel(OpenAiImageApi.builder().apiKey(apiKey).baseUrl(url).restClientBuilder(rest.clone()).build(),
            OpenAiImageOptions.builder().model(imageModel).N(1).width(1024).height(1024).build(),retry);
        var audio=OpenAiAudioApi.builder().apiKey(apiKey).baseUrl(url).restClientBuilder(rest.clone()).build();
        transcription=new OpenAiAudioTranscriptionModel(audio,OpenAiAudioTranscriptionOptions.builder().model(transcriptionModel).language("ko")
            .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.JSON).build(),retry);
        speech=new OpenAiAudioSpeechModel(audio,OpenAiAudioSpeechOptions.builder().model(speechModel).voice("alloy").build(),retry);
        moderation=new OpenAiModerationModel(OpenAiModerationApi.builder().apiKey(apiKey).baseUrl(url).restClientBuilder(rest.clone()).build(),retry)
            .withDefaultOptions(OpenAiModerationOptions.builder().model(moderationModel).build());
    }
}
