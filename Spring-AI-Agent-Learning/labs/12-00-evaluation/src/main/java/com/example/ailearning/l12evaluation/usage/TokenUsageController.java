package com.example.ailearning.l12evaluation.usage;

import jakarta.validation.constraints.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class TokenUsageController {
    private final TokenUsageService tokenUsageService;

    public TokenUsageController(TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
    }

    @PostMapping(value = "/api/evaluation/usage", consumes = MediaType.TEXT_PLAIN_VALUE)
    public TokenUsageService.Reply ask(@RequestBody @NotBlank @Size(max = 1000) String question) {
        return tokenUsageService.ask(question);
    }
}
