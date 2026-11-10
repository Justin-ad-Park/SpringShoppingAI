package com.example.ailearning.l13intent;

import com.example.ailearning.l13intent.domain.IntentCatalog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/intents")
public class IntentClassificationController {
    private final IntentClassificationService service;
    private final IntentCatalog catalog;

    public IntentClassificationController(IntentClassificationService service, IntentCatalog catalog) {
        this.service = service;
        this.catalog = catalog;
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public IntentClassificationService.Result classify(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return service.classify(question);
    }

    @GetMapping("/catalog")
    public IntentCatalog catalog() {
        return catalog;
    }
}
