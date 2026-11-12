package com.example.ailearning.l12observe;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ObservedChatService {
    private final ChatClient chatClient;
    public ObservedChatService(ChatClient.Builder builder) { chatClient = builder.build(); }

    public String ask(String question) {
        return chatClient
                .prompt()
                .user(question)
                .call()
                .content();
    }
}
