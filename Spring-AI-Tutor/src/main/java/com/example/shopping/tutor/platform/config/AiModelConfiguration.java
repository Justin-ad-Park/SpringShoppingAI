package com.example.shopping.tutor.platform.config;

import com.example.shopping.tutor.lesson.l07vector.support.FixedEmbeddingModel;
import com.example.shopping.tutor.platform.stub.ScriptedChatModel;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;





/** 서버에서만 자격증명을 소유한다. stub 시작에는 API 키나 외부 서비스가 필요 없다. */
@Configuration
public class AiModelConfiguration {
    public record Settings(
            String mode,
            String model,
            String embeddingModel,
            String mcpUrl,
            boolean nativeSchema) {}

    @Bean
    Settings settings(
            @Value("${tutor.mode}") String mode,
            @Value("${tutor.chat-model}") String model,
            @Value("${tutor.embedding-model}") String embedding,
            @Value("${tutor.mcp-url}") String mcp,
            @Value("${tutor.native-schema}") boolean nativeSchema) {
        if (!mode.equals("stub") && !mode.equals("live"))
            throw new IllegalArgumentException("TUTOR_MODE는 stub/live입니다.");
        return new Settings(mode, model, embedding, mcp, nativeSchema);
    }

    public record Connection(String key, String url) {}

    /** 2.0 SDK는 base URL에 /v1을 포함한다. 기존 origin 설정도 계속 허용한다. */
    public static String sdkBaseUrl(String url) {
        String base = url.replaceAll("/+$", "");
        return base.endsWith("/v1") ? base : base + "/v1";
    }

    @Bean
    Connection openAiConnection(Settings settings,
            @Value("${tutor.api-key}") String key,
            @Value("${tutor.base-url}") String baseUrl) {
        if (settings.mode().equals("live") && key.isBlank())
            throw new IllegalArgumentException("live 모드에는 OPENAI_API_KEY가 필요합니다.");
        return new Connection(key.isBlank() ? "stub-not-a-key" : key, sdkBaseUrl(baseUrl));
    }

    @Bean
    ChatModel chatModel(Settings s, Connection api, ObservationRegistry registry) {
        if (s.mode().equals("stub")) return new ScriptedChatModel();
        return com.example.shopping.tutor.lesson.l00setup.ChatModelConfiguration.create(
                api.key(), api.url(), s.model(), registry);
    }

    @Bean
    EmbeddingModel embeddingModel(Settings s, Connection api, ObservationRegistry registry) {
        return s.mode().equals("stub")
                ? new FixedEmbeddingModel()
                : com.example.shopping.tutor.lesson.l07vector.EmbeddingModelConfiguration.create(
                        api.key(), api.url(), s.embeddingModel(), registry);
    }
}
