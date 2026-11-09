package com.example.ailearning.l06decisions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class ComparisonController {
    private final ComparisonService service;
    public ComparisonController(ComparisonService service) { this.service = service; }

    @PostMapping(value = "/api/compare/{mode}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public RunResult compare(@PathVariable ExecutionMode mode,
            @RequestBody @NotBlank @Size(max = 2000) String question) {
        return service.run(mode, question);
    }
}
