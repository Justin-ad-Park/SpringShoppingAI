package com.example.ailearning.l11judge;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluate")
public class EvaluationController {
    public record Candidate(@NotBlank String question, @NotBlank String context, @NotBlank String answer) {}
    private final AnswerEvaluator evaluator;
    public EvaluationController(AnswerEvaluator evaluator) { this.evaluator = evaluator; }

    @PostMapping("/relevance")
    public boolean relevance(@RequestBody @Valid Candidate candidate) {
        return evaluator.relevance(candidate.question(), candidate.context(), candidate.answer());
    }

    @PostMapping("/facts")
    public boolean facts(@RequestBody @Valid Candidate candidate) {
        return evaluator.facts(candidate.context(), candidate.answer());
    }
}
