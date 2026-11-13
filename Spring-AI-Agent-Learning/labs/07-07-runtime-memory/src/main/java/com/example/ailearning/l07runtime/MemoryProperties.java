package com.example.ailearning.l07runtime;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("learning.memory")
public record MemoryProperties(@Min(2) int maxMessages, @NotNull @Valid Redis redis) {
    public record Redis(@NotNull URI uri, @NotBlank String indexName, @NotBlank String keyPrefix,
                        @NotNull Duration timeToLive, boolean initializeSchema) {}
}
