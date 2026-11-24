package com.example.ailearning.l09search.lesson01_vectors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class Lesson01EmbeddingController {
    private final Lesson01EmbeddingService embeddingService;

    public Lesson01EmbeddingController(Lesson01EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping(value = "/api/embedding", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Lesson01EmbeddingService.EmbeddingVector embed(@RequestBody @NotBlank @Size(max = 500) String text) {
        return embeddingService.embed(text);
    }
}
