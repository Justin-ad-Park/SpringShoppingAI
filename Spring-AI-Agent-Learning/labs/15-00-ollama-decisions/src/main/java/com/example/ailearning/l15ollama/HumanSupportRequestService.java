package com.example.ailearning.l15ollama;

import com.example.ailearning.l15ollama.domain.HumanSupportAction;
import com.example.ailearning.l15ollama.domain.SupportRoutingPolicy;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionPrompt;
import org.springframework.ai.decision.question.YesNoQuestion;
import org.springframework.stereotype.Service;

/** 15-3: 사람 상담원 연결을 원하는지 참일 확률로 판단한다. */
@Service
public class HumanSupportRequestService {
    public record HumanSupportResult(double yesProbability, HumanSupportAction action) {}

    static final YesNoQuestion HUMAN_SUPPORT_QUESTION = YesNoQuestion.builder()
            .text("고객이 사람 상담원 연결을 요청하나요?")
            .whenYes("사람 상담원과 직접 대화하고 싶다는 요청")
            .whenNo("사람 상담원 연결을 요청하지 않거나 자동 안내를 원하는 문의")
            .build();
    private final DecisionModel decisionModel;

    public HumanSupportRequestService(DecisionModel decisionModel) {
        this.decisionModel = decisionModel;
    }

    public HumanSupportResult assessHumanSupportRequest(String message) {
        var prompt = DecisionPrompt.builder()
                .text(message)
                .question("human_support", HUMAN_SUPPORT_QUESTION)
                .build();
        var response = decisionModel.call(prompt);
        var answer = response.getYesNo("human_support");
        return new HumanSupportResult(answer.getProbability(),
                SupportRoutingPolicy.determineHumanSupportAction(answer.getProbability()));
    }
}
