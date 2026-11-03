package com.example.ailearning.l02multiple;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientsConfiguration {
    @Bean("openAiChatClient")
    public ChatClient openAiChatClient(OpenAiChatModel model) {
        return ChatClient.create(model);
    }

    @Bean("anthropicChatClient")
    public ChatClient anthropicChatClient(AnthropicChatModel model) {
        return ChatClient.create(model);
    }
}
