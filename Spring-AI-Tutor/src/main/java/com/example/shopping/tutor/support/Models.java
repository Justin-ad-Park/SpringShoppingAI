package com.example.shopping.tutor.support;

import java.time.Duration;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.document.MetadataMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.retry.support.RetryTemplate;

/** 서버에서만 자격증명을 소유한다. stub 시작에는 API 키나 외부 서비스가 필요 없다. */
@Configuration
public class Models {
    public record Settings(String mode, String model, String embeddingModel, String mcpUrl, boolean nativeSchema) {}
    @Bean Settings settings(@Value("${tutor.mode}") String mode, @Value("${tutor.chat-model}") String model,
            @Value("${tutor.embedding-model}") String embedding, @Value("${tutor.mcp-url}") String mcp,
            @Value("${tutor.native-schema}") boolean nativeSchema) {
        if (!mode.equals("stub") && !mode.equals("live")) throw new IllegalArgumentException("TUTOR_MODE는 stub/live입니다.");
        return new Settings(mode,model,embedding,mcp,nativeSchema);
    }
    @Bean OpenAiApi openAiApi(Settings settings, @Value("${tutor.api-key}") String key, @Value("${tutor.base-url}") String baseUrl) {
        if(settings.mode().equals("live") && key.isBlank()) throw new IllegalArgumentException("live 모드에는 OPENAI_API_KEY가 필요합니다.");
        var factory=new JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(45));
        return OpenAiApi.builder().apiKey(key.isBlank()?"stub-not-a-key":key).baseUrl(baseUrl)
            .restClientBuilder(RestClient.builder().requestFactory(factory)).build();
    }
    @Bean ChatModel chatModel(Settings s, OpenAiApi api, ObservationRegistry registry) {
        if(s.mode().equals("stub")) return new ScriptedChatModel();
        return OpenAiChatModel.builder().openAiApi(api)
            .defaultOptions(OpenAiChatOptions.builder().model(s.model()).maxTokens(800).build())
            .retryTemplate(RetryTemplate.builder().maxAttempts(1).build()).observationRegistry(registry).build();
    }
    @Bean EmbeddingModel embeddingModel(Settings s, OpenAiApi api, ObservationRegistry registry) {
        return s.mode().equals("stub")?new FixedEmbeddingModel():new OpenAiEmbeddingModel(api, MetadataMode.EMBED,
            OpenAiEmbeddingOptions.builder().model(s.embeddingModel()).build(),RetryTemplate.builder().maxAttempts(1).build(),registry);
    }
}
