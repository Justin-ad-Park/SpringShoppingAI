package com.example.ailearning.l09search;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class EmbeddingController {
    private final EmbeddingService embeddingService;

    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping(value = "/api/embedding", consumes = MediaType.TEXT_PLAIN_VALUE)
    public EmbeddingService.EmbeddingVector embed(@RequestBody @NotBlank @Size(max = 500) String text) {
        return embeddingService.embed(text);
    }
}
