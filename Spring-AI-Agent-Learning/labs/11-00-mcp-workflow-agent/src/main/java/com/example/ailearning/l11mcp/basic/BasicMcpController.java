package com.example.ailearning.l11mcp.basic;

import jakarta.validation.constraints.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class BasicMcpController {
    private final BasicMcpChatService basicMcpChatService;

    public BasicMcpController(BasicMcpChatService basicMcpChatService) { this.basicMcpChatService = basicMcpChatService; }

    @PostMapping(value = "/api/mcp/basic", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String ask(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return basicMcpChatService.ask(question);
    }
}
