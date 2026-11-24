package com.example.ailearning.l15ollama;

import com.example.ailearning.l15ollama.domain.ShoppingIntent;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionPrompt;
import org.springframework.stereotype.Service;

/** 15-5: 앞에서 배운 세 질문을 한 번의 모델 요청으로 보낸다. */
@Service
public class CustomerAssessmentService {
    public record CustomerAssessment(ShoppingIntent intent, double humanSupportProbability, double urgencyScore) {}
    private final DecisionModel decisionModel;

    public CustomerAssessmentService(DecisionModel decisionModel) {
        this.decisionModel = decisionModel;
    }

    public CustomerAssessment assessCustomerMessage(String message) {
        var prompt = DecisionPrompt.builder()
                .text(message)
                .question("intent", IntentClassificationService.INTENT_QUESTION)
                .question("human_support", HumanSupportRequestService.HUMAN_SUPPORT_QUESTION)
                .question("urgency", SupportPriorityService.URGENCY_QUESTION)
                .build();
        var response = decisionModel.call(prompt);
        return new CustomerAssessment(ShoppingIntent.valueOf(response.getChoice("intent").getValue()),
                response.getYesNo("human_support").getProbability(), response.getScore("urgency").getValue());
    }
}
