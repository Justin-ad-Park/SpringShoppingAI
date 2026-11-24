package com.example.ailearning.l09search.lesson02_store;

import java.util.List;
import jakarta.validation.constraints.*;
import org.springframework.ai.document.Document;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("basic-search")
public class Lesson02BasicSearchController {
    private final Lesson02BasicVectorSearchService searchService;

    public Lesson02BasicSearchController(Lesson02BasicVectorSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/search/basic")
    public List<Document> search(@RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "2") @Min(1) @Max(3) int topK) {
        return searchService.search(query, topK);
    }
}
