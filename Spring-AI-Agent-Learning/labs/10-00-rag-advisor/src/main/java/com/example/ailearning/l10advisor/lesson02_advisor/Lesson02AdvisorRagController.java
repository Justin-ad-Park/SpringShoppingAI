package com.example.ailearning.l10advisor.lesson02_advisor;

import org.springframework.context.annotation.Profile;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@Profile("!indexing-only")
@RestController
public class Lesson02AdvisorRagController {
    private final Lesson02AdvisorRagService service;
    public Lesson02AdvisorRagController(Lesson02AdvisorRagService service) { this.service = service; }

    @PostMapping(value = "/api/advisor-rag", consumes = "text/plain", produces = "text/plain;charset=UTF-8")
    public String answer(@RequestBody @NotBlank String question) { return service.answer(question); }
}
