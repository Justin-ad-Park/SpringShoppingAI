package com.example.ailearning.l12evaluation.lesson03_usage;

import jakarta.validation.constraints.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class Lesson03TokenUsageController {
    private final Lesson03TokenUsageService tokenUsageService;

    public Lesson03TokenUsageController(Lesson03TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
    }

    @PostMapping(value = "/api/evaluation/usage", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Lesson03TokenUsageService.Reply ask(@RequestBody @NotBlank @Size(max = 1000) String question) {
        return tokenUsageService.ask(question);
    }
}
