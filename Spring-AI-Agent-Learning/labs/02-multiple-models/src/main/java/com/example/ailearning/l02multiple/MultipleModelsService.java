package com.example.ailearning.l02multiple;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class MultipleModelsService {
    private final ChatClient openAi;
    private final ChatClient anthropic;

    public MultipleModelsService(
            @Qualifier("openAiChatClient") ChatClient openAi,
            @Qualifier("anthropicChatClient") ChatClient anthropic) {
        this.openAi = openAi;
        this.anthropic = anthropic;
    }

    public String askOpenAi(String question) {
        return openAi
                .prompt()
                .user(question)
                .call()
                .content();
    }

    public String askAnthropic(String question) {
        return anthropic
                .prompt()
                .user(question)
                .call()
                .content();
    }
}
