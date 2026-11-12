package com.example.ailearning.l12evaluation;

import com.example.ailearning.l12evaluation.policy.PolicySearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class EvaluationRunner {
    public record EvaluationCase(String id, String question, String expectedStatus,
            List<String> expectedSourceIds, String expectedText) {}
    public record EvaluationCaseResult(String id, boolean passed, List<String> failures,
            RagService.RagRunResult actual) {}
    public record EvaluationReport(int total, int passed, double passRate, List<EvaluationCaseResult> cases) {}

    private final RagService ragService;
    private final List<EvaluationCase> evaluationCases;

    public EvaluationRunner(RagService ragService, ObjectMapper objectMapper) throws IOException {
        this.ragService = ragService;
        try (var input = new ClassPathResource("evaluation-cases.json").getInputStream()) {
            evaluationCases = List.of(objectMapper.readValue(input, EvaluationCase[].class));
        }
    }

    public EvaluationReport run() {
        List<EvaluationCaseResult> caseResults = evaluationCases.stream()
                .map(this::evaluateCase)
                .toList();
        int passedCaseCount = (int) caseResults.stream()
                .filter(EvaluationCaseResult::passed)
                .count();
        return new EvaluationReport(caseResults.size(), passedCaseCount,
                (double) passedCaseCount / caseResults.size(), caseResults);
    }

    /** 문의 한 건을 실행하고 기대한 상태·출처·본문과 대조한다. */
    private EvaluationCaseResult evaluateCase(EvaluationCase evaluationCase) {
        RagService.RagRunResult actualResult = ragService.answer(evaluationCase.question());
        RagService.Reply actualReply = actualResult.reply();
        List<String> failures = new ArrayList<>();
        if (!actualReply.status().equals(evaluationCase.expectedStatus())) {
            failures.add("상태 불일치");
        }
        List<String> actualSourceIds = actualReply.sources().stream()
                .map(PolicySearchResult::id)
                .toList();
        if (!actualSourceIds.containsAll(evaluationCase.expectedSourceIds())) {
            failures.add("expected source ID missing");
        }
        if (!actualReply.answer().contains(evaluationCase.expectedText())) {
            failures.add("expected text missing");
        }
        return new EvaluationCaseResult(evaluationCase.id(), failures.isEmpty(), List.copyOf(failures), actualResult);
    }
}
