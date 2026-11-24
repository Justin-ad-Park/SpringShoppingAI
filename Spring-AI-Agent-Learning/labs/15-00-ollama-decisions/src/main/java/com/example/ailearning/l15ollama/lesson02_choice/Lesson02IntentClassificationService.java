package com.example.ailearning.l15ollama.lesson02_choice;

import com.example.ailearning.l15ollama.domain.ShoppingIntent;
import com.example.ailearning.l15ollama.domain.SupportRoutingPolicy;
import java.util.Map;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionPrompt;
import org.springframework.ai.decision.question.ChoiceQuestion;
import org.springframework.stereotype.Service;

/** 15-2: 정해진 쇼핑 의도 중 하나를 선택한다. */
@Service
public class Lesson02IntentClassificationService {
    public record IntentResult(ShoppingIntent predictedIntent, Double confidence,
                               Map<String, Double> probabilities, boolean clarificationRequired) {}

    public static final ChoiceQuestion INTENT_QUESTION = ChoiceQuestion.builder()
            .text("고객 메시지의 주된 쇼핑 의도를 하나 선택하세요.")
            .option("PRODUCT_SEARCH", "상품 검색, 추천, 상품 특징이나 가격 문의")
            .option("ORDER_DELIVERY", "이미 주문한 상품의 주문 상태 또는 배송 조회")
            .option("RETURN_EXCHANGE", "구매한 상품의 반품 또는 교환 문의")
            .option("OTHER", "위 항목에 해당하지 않거나 여러 의도가 섞인 요청")
            .build();
    private final DecisionModel decisionModel;

    public Lesson02IntentClassificationService(DecisionModel decisionModel) {
        this.decisionModel = decisionModel;
    }

    public IntentResult classifyIntent(String message) {
        var prompt = DecisionPrompt.builder()
                .text(message)
                .question("intent", INTENT_QUESTION)
                .build();
        var response = decisionModel.call(prompt);
        var answer = response.getChoice("intent");
        var intent = ShoppingIntent.valueOf(answer.getValue());
        return new IntentResult(intent, answer.getConfidence(), answer.getProbabilities(),
                SupportRoutingPolicy.requiresIntentClarification(intent, answer.getConfidence()));
    }
}
