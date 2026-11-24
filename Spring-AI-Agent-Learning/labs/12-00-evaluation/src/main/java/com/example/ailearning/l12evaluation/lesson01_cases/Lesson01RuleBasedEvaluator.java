package com.example.ailearning.l12evaluation.lesson01_cases;

import java.util.ArrayList;
import java.util.List;

/** 12-1: 준비된 답변을 기대값과 비교한다. 답변 생성·검색·계측은 하지 않는다. */
public class Lesson01RuleBasedEvaluator {
    public record ExpectedAnswer(String status, List<String> sourceIds, String requiredText) {}
    public record CandidateAnswer(String status, List<String> sourceIds, String text) {}

    public List<String> evaluate(ExpectedAnswer expected, CandidateAnswer actual) {
        var failures = new ArrayList<String>();
        if (!actual.status().equals(expected.status())) failures.add("상태 불일치");
        if (!actual.sourceIds().containsAll(expected.sourceIds())) failures.add("expected source ID missing");
        if (!actual.text().contains(expected.requiredText())) failures.add("expected text missing");
        return List.copyOf(failures);
    }
}
