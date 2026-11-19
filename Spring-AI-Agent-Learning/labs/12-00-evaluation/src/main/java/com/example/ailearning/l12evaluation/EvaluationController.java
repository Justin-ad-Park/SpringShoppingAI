package com.example.ailearning.l12evaluation;

import org.springframework.context.annotation.Profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Profile("!usage-only")
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {
    private final RagService ragService;
    private final EvaluationRunner evaluationRunner;

    public EvaluationController(RagService ragService, EvaluationRunner evaluationRunner) {
        this.ragService = ragService;
        this.evaluationRunner = evaluationRunner;
    }

    @PostMapping(value = "/ask", consumes = MediaType.TEXT_PLAIN_VALUE)
    public RagService.RagRunResult ask(@RequestBody @NotBlank @Size(max = 1000) String question) {
        return ragService.answer(question);
    }

    @PostMapping("/run")
    public EvaluationRunner.EvaluationReport run() {
        return evaluationRunner.run();
    }
}
