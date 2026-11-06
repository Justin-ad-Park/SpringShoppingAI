package com.example.ailearning.l04structured;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/criteria")
public class CriteriaController {
    private final CriteriaService service;

    public CriteriaController(CriteriaService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public SearchCriteria extract(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return service.extract(question);
    }

    @PostMapping(value = "/text", consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String text(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return service.text(question);
    }
}
