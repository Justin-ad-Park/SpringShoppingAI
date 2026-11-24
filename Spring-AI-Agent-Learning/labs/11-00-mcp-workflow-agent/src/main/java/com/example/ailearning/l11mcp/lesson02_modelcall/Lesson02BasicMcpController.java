package com.example.ailearning.l11mcp.lesson02_modelcall;

import jakarta.validation.constraints.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class Lesson02BasicMcpController {
    private final Lesson02BasicMcpChatService basicMcpChatService;

    public Lesson02BasicMcpController(Lesson02BasicMcpChatService basicMcpChatService) { this.basicMcpChatService = basicMcpChatService; }

    @PostMapping(value = "/api/mcp/basic", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String ask(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return basicMcpChatService.ask(question);
    }
}
