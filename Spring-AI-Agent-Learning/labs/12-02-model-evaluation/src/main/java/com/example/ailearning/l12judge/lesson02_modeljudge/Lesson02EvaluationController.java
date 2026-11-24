package com.example.ailearning.l12judge.lesson02_modeljudge;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluate")
public class Lesson02EvaluationController {
    public record Candidate(@NotBlank String question, @NotBlank String context, @NotBlank String answer) {}
    private final Lesson02AnswerEvaluator answerEvaluator;
    public Lesson02EvaluationController(Lesson02AnswerEvaluator answerEvaluator) { this.answerEvaluator = answerEvaluator; }

    @PostMapping("/relevance")
    public boolean relevance(@RequestBody @Valid Candidate candidate) {
        return answerEvaluator.evaluateRelevance(candidate.question(), candidate.context(), candidate.answer());
    }

    @PostMapping("/facts")
    public boolean facts(@RequestBody @Valid Candidate candidate) {
        return answerEvaluator.evaluateFactualConsistency(candidate.context(), candidate.answer());
    }
}
