package com.example.ailearning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    public static final int MAX_MESSAGE_LENGTH = 2000;

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/api/chat", consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String chat(@RequestBody @NotBlank @Size(max = MAX_MESSAGE_LENGTH) String message) {
        return chatService.ask(message);
    }
}
