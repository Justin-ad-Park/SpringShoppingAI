package com.example.ailearning.l09search;

import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SearchController {
    private final SearchService service;
    public SearchController(SearchService service) { this.service = service; }

    @GetMapping("/api/search")
    public SearchService.Comparison search(@RequestParam @NotBlank @Size(max = 500) String query,
            @RequestParam(defaultValue = "2") @Min(1) @Max(3) int topK,
            @RequestParam(defaultValue = "0.5") @DecimalMin("0.0") @DecimalMax("1.0") double threshold,
            @RequestParam(required = false) String category) {
        try { return service.search(query, topK, threshold, category); }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage()); }
    }

    @PostMapping(value = "/api/embedding", consumes = MediaType.TEXT_PLAIN_VALUE)
    public SearchService.Vector embed(@RequestBody @NotBlank @Size(max = 500) String text) { return service.embed(text); }
}
