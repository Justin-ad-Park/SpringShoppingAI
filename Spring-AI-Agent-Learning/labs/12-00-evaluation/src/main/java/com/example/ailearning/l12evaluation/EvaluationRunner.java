package com.example.ailearning.l12evaluation;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationRunner {
    public record EvaluationCase(String id, String question, String expectedStatus,
                                 List<String> expectedSourceIds, String expectedText) {}
    public record CaseResult(String id, boolean passed, List<String> failures, RagService.Run actual) {}
    public record Report(int total, int passed, double passRate, List<CaseResult> cases) {}
    private final RagService service;
    private final List<EvaluationCase> cases;

    public EvaluationRunner(RagService service, ObjectMapper mapper) throws IOException {
        this.service = service;
        try (var input = new ClassPathResource("evaluation-cases.json").getInputStream()) {
            cases = List.of(mapper.readValue(input, EvaluationCase[].class));
        }
    }

    public Report run() {
        var results = new ArrayList<CaseResult>();
        for (var example : cases) {
            RagService.Run actual = service.answer(example.question());
            var reply = actual.reply();
            var failures = new ArrayList<String>();
            if (!reply.status().equals(example.expectedStatus())) {
                failures.add("상태 불일치");
            }

            var sourceIds = reply.sources()
                    .stream()
                    .map(Source::id)
                    .toList();
            if (!sourceIds.containsAll(example.expectedSourceIds())) {
                failures.add("expected source ID missing");
            }
            if (!reply.answer().contains(example.expectedText())) {
                failures.add("expected text missing");
            }

            results.add(new CaseResult(example.id(), failures.isEmpty(), List.copyOf(failures), actual));
        }
        int passed = (int) results
                .stream()
                .filter(CaseResult::passed)
                .count();
        return new Report(results.size(), passed, (double) passed / results.size(), List.copyOf(results));
    }
}
