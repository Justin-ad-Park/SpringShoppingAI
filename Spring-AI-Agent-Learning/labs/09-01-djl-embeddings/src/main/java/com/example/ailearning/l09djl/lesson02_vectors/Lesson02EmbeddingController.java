package com.example.ailearning.l09djl.lesson02_vectors;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/embedding")
public class Lesson02EmbeddingController {
    public record EmbeddingVector(int dimensions, float[] values) {}
    private final Lesson02DjlEmbeddingService embeddingService;

    public Lesson02EmbeddingController(Lesson02DjlEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public EmbeddingVector embed(@RequestBody @NotBlank String text) {
        float[] vector = embeddingService.embedQuery(text);
        return new EmbeddingVector(vector.length, vector);
    }
}
