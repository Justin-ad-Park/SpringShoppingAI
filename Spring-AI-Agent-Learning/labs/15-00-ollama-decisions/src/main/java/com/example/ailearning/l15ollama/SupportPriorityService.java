package com.example.ailearning.l15ollama;

import com.example.ailearning.l15ollama.domain.SupportPriority;
import com.example.ailearning.l15ollama.domain.SupportRoutingPolicy;
import java.util.List;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionPrompt;
import org.springframework.ai.decision.question.ScoreQuestion;
import org.springframework.stereotype.Service;

/** 15-4: 순서가 있는 세 기준으로 시급성을 평가한다. */
@Service
public class SupportPriorityService {
    public record PriorityResult(double score, int nearestLevel, Double confidence,
                                 List<Double> probabilities, SupportPriority priority) {}

    static final ScoreQuestion URGENCY_QUESTION = ScoreQuestion.of("얼마나 시급한 고객 문의인가요?",
            "일반 문의: 시간 제약이나 현재 문제가 없는 정보 문의",
            "현재 문제: 고객이 불편을 겪지만 임박한 마감은 없음",
            "즉시 확인 필요: 오늘 마감이나 즉시 처리해야 할 문제가 있음");
    private final DecisionModel decisionModel;

    public SupportPriorityService(DecisionModel decisionModel) {
        this.decisionModel = decisionModel;
    }

    public PriorityResult assessPriority(String message) {
        var prompt = DecisionPrompt.builder()
                .text(message)
                .question("urgency", URGENCY_QUESTION)
                .build();
        var response = decisionModel.call(prompt);
        var answer = response.getScore("urgency");
        return new PriorityResult(answer.getValue(), answer.getNearestLevel(), answer.getConfidence(),
                answer.getProbabilities(), SupportRoutingPolicy.determinePriority(answer.getValue(), answer.getConfidence()));
    }
}
