package com.example.ailearning.l10advisor;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
public class AdvisorRagController {
    private final AdvisorRagService service;
    public AdvisorRagController(AdvisorRagService service) { this.service = service; }

    @PostMapping(value = "/api/advisor-rag", consumes = "text/plain", produces = "text/plain;charset=UTF-8")
    public String answer(@RequestBody @NotBlank String question) { return service.answer(question); }
}
