package com.example.ailearning.l12evaluation.lesson01_cases;

import com.example.ailearning.l12evaluation.RagService;

import org.springframework.context.annotation.Profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Profile("!usage-only")
@RestController
@RequestMapping("/api/evaluation")
public class Lesson01EvaluationController {
    private final RagService ragService;
    private final Lesson01EvaluationRunner evaluationRunner;

    public Lesson01EvaluationController(RagService ragService, Lesson01EvaluationRunner evaluationRunner) {
        this.ragService = ragService;
        this.evaluationRunner = evaluationRunner;
    }

    @PostMapping(value = "/ask", consumes = MediaType.TEXT_PLAIN_VALUE)
    public RagService.RagRunResult ask(@RequestBody @NotBlank @Size(max = 1000) String question) {
        return ragService.answer(question);
    }

    @PostMapping("/run")
    public Lesson01EvaluationRunner.EvaluationReport run() {
        return evaluationRunner.run();
    }
}
