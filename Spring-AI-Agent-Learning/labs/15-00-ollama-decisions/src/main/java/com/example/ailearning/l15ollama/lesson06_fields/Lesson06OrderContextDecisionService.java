package com.example.ailearning.l15ollama.lesson06_fields;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionPrompt;
import org.springframework.ai.decision.question.ChoiceQuestion;
import org.springframework.stereotype.Service;

/** 15-6: 고객 메시지와 주문 정보를 이름 붙은 필드로 함께 전달한다. */
@Service
public class Lesson06OrderContextDecisionService {
    public record OrderInquiry(@NotBlank @Size(max = 2000) String message,
                               @NotBlank @Size(max = 40) String orderStatus,
                               @Min(0) int daysSinceDelivery) {}
    public record OrderContextResult(String nextAction, Map<String, Double> probabilities) {}
    private final DecisionModel decisionModel;

    public Lesson06OrderContextDecisionService(DecisionModel decisionModel) {
        this.decisionModel = decisionModel;
    }

    public OrderContextResult decideNextAction(OrderInquiry inquiry) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("message", inquiry.message());
        fields.put("orderStatus", inquiry.orderStatus());
        fields.put("daysSinceDelivery", inquiry.daysSinceDelivery());
        var prompt = DecisionPrompt.builder()
                .fields(fields)
                .question("next_action", ChoiceQuestion.builder()
                        .text("고객 메시지와 주문 상태를 함께 보고 어떤 안내가 필요한지 고르세요.")
                        .option("ORDER_PAYMENT", "주문 또는 결제 관련 문의")
                        .option("EXPLAIN_RETURN", "배송이 완료된 상품의 반품 방법을 묻는 문의")
                        .option("OTHER_QUESTION", "단순 문의 또는 서비스 정책 문의")
                        .build())
                .build();
        var response = decisionModel.call(prompt);
        var answer = response.getChoice("next_action");
        return new OrderContextResult(answer.getValue(), answer.getProbabilities());
    }
}
