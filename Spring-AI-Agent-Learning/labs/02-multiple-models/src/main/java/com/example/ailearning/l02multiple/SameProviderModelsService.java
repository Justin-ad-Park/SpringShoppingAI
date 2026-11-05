package com.example.ailearning.l02multiple;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SameProviderModelsService {
    private final ChatClient openAi;

    public SameProviderModelsService(
            @Qualifier("openAiChatClient") ChatClient openAi) {
        this.openAi = openAi;
    }

    public String ask(String question, String modelId) {
        return openAi
                .prompt()
                .user(question)
                .options(OpenAiChatOptions.builder()
                        .model(modelId))
                .call()
                .content();
    }
}
