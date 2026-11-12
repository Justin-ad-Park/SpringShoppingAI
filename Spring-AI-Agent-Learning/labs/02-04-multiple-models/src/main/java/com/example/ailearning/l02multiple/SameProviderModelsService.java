package com.example.ailearning.l02multiple;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SameProviderModelsService {
    private final ChatClient openAiChatClient;

    public SameProviderModelsService(
            @Qualifier("openAiChatClient") ChatClient openAiChatClient) {
        this.openAiChatClient = openAiChatClient;
    }

    public String ask(String question, String modelId) {
        return openAiChatClient
                .prompt()
                .user(question)
                .options(OpenAiChatOptions.builder()
                        .model(modelId))
                .call()
                .content();
    }
}
