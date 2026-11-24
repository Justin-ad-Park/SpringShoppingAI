package com.example.ailearning.l12observe.lesson04_observability;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class Lesson04ObservedChatService {
    private final ChatClient chatClient;
    public Lesson04ObservedChatService(ChatClient.Builder builder) { chatClient = builder.build(); }

    public String ask(String question) {
        return chatClient
                .prompt()
                .user(question)
                .call()
                .content();
    }
}
