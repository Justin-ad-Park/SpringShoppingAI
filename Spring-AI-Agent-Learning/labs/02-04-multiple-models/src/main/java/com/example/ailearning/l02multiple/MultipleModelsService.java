package com.example.ailearning.l02multiple;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class MultipleModelsService {
    private final ChatClient openAiChatClient;
    private final ChatClient anthropicChatClient;

    public MultipleModelsService(
            @Qualifier("openAiChatClient") ChatClient openAiChatClient,
            @Qualifier("anthropicChatClient") ChatClient anthropicChatClient) {
        this.openAiChatClient = openAiChatClient;
        this.anthropicChatClient = anthropicChatClient;
    }

    public String askOpenAi(String question) {
        return openAiChatClient
                .prompt()
                .user(question)
                .call()
                .content();
    }

    public String askAnthropic(String question) {
        return anthropicChatClient
                .prompt()
                .user(question)
                .call()
                .content();
    }
}
