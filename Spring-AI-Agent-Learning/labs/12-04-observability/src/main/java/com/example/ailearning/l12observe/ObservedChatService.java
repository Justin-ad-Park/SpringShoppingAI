package com.example.ailearning.l12observe;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ObservedChatService {
    private final ChatClient client;
    public ObservedChatService(ChatClient.Builder builder) { client = builder.build(); }

    public String ask(String question) {
        return client
                .prompt()
                .user(question)
                .call()
                .content();
    }
}
