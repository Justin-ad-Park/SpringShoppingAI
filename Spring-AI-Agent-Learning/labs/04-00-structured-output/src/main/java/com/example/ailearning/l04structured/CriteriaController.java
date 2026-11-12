package com.example.ailearning.l04structured;

import com.example.ailearning.l04structured.domain.SearchCriteria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/criteria")
public class CriteriaController {
    private final CriteriaService criteriaService;

    public CriteriaController(CriteriaService criteriaService) {
        this.criteriaService = criteriaService;
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public SearchCriteria extractCriteria(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return criteriaService.extractCriteria(question);
    }

    @PostMapping(value = "/text", consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String extractCriteriaAsText(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return criteriaService.extractCriteriaAsText(question);
    }
}
