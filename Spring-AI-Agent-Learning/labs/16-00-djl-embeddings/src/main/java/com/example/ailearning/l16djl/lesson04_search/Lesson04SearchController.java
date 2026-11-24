package com.example.ailearning.l16djl.lesson04_search;

import jakarta.validation.constraints.*;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@Profile("!embedding-only")
@RestController
@RequestMapping("/api/search")
public class Lesson04SearchController {
    private final Lesson04SearchService searchService;

    public Lesson04SearchController(Lesson04SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public Lesson04SearchService.SearchComparison search(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "2") @Min(1) @Max(10) int topK,
            @RequestParam(defaultValue = "0.0") @DecimalMin("0.0") @DecimalMax("1.0") double threshold,
            @RequestParam(required = false) @Pattern(regexp = "shipping|returns") String category) {
        return searchService.compareSearchResults(query, topK, threshold, category);
    }
}
