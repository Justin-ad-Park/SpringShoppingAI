package com.example.ailearning.l09search;

import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SearchController {
    private final SearchService searchService;
    public SearchController(SearchService searchService) { this.searchService = searchService; }

    @GetMapping("/api/search")
    public SearchService.SearchComparison compareSearchResults(@RequestParam @NotBlank @Size(max = 500) String query,
            @RequestParam(defaultValue = "2") @Min(1) @Max(3) int topK,
            @RequestParam(defaultValue = "0.5") @DecimalMin("0.0") @DecimalMax("1.0") double threshold,
            @RequestParam(required = false) String category) {
        try { return searchService.compareSearchResults(query, topK, threshold, category); }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage()); }
    }

    @PostMapping(value = "/api/embedding", consumes = MediaType.TEXT_PLAIN_VALUE)
    public SearchService.EmbeddingVector embed(@RequestBody @NotBlank @Size(max = 500) String text) { return searchService.embed(text); }
}
