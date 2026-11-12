package com.example.ailearning.l04structured;

import com.example.ailearning.l04structured.domain.ProductSearchCondition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/criteria")
public class SearchConditionController {
    private final SearchConditionExtractor searchConditionExtractor;

    public SearchConditionController(SearchConditionExtractor searchConditionExtractor) {
        this.searchConditionExtractor = searchConditionExtractor;
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public ProductSearchCondition extractSearchCondition(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return searchConditionExtractor.extractSearchCondition(question);
    }

    @PostMapping(value = "/text", consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String extractSearchConditionAsText(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return searchConditionExtractor.extractSearchConditionAsText(question);
    }
}
