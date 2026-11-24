package com.example.ailearning.l09search.lesson03_search;

import org.springframework.context.annotation.Profile;

import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Profile("!embedding-only & !basic-search")
@RestController
public class Lesson03SearchController {
    private final Lesson03SearchService searchService;
    public Lesson03SearchController(Lesson03SearchService searchService) { this.searchService = searchService; }

    @GetMapping("/api/search")
    public Lesson03SearchService.SearchComparison compareSearchResults(@RequestParam @NotBlank @Size(max = 500) String query,
            @RequestParam(defaultValue = "2") @Min(1) @Max(3) int topK,
            @RequestParam(defaultValue = "0.5") @DecimalMin("0.0") @DecimalMax("1.0") double threshold,
            @RequestParam(required = false) String category) {
        try { return searchService.compareSearchResults(query, topK, threshold, category); }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage()); }
    }

}
