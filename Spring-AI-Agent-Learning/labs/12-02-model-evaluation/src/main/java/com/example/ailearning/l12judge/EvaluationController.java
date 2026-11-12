package com.example.ailearning.l12judge;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluate")
public class EvaluationController {
    public record Candidate(@NotBlank String question, @NotBlank String context, @NotBlank String answer) {}
    private final AnswerEvaluator answerEvaluator;
    public EvaluationController(AnswerEvaluator answerEvaluator) { this.answerEvaluator = answerEvaluator; }

    @PostMapping("/relevance")
    public boolean relevance(@RequestBody @Valid Candidate candidate) {
        return answerEvaluator.evaluateRelevance(candidate.question(), candidate.context(), candidate.answer());
    }

    @PostMapping("/facts")
    public boolean facts(@RequestBody @Valid Candidate candidate) {
        return answerEvaluator.evaluateFactualConsistency(candidate.context(), candidate.answer());
    }
}
