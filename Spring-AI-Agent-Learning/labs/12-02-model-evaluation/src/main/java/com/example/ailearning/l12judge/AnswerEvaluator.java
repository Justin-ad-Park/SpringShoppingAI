package com.example.ailearning.l12judge;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AnswerEvaluator {
    private final RelevancyEvaluator relevancyEvaluator;
    private final FactCheckingEvaluator factCheckingEvaluator;

    public AnswerEvaluator(ChatClient.Builder builder) {
        relevancyEvaluator = new RelevancyEvaluator(builder.clone());
        factCheckingEvaluator = FactCheckingEvaluator.builder(builder.clone())
                .build();
    }

    public boolean evaluateRelevance(String question, String context, String answer) {
        var evaluationRequest = new EvaluationRequest(question, List.of(new Document(context)), answer);
        return relevancyEvaluator
                .evaluate(evaluationRequest)
                .isPass();
    }

    public boolean evaluateFactualConsistency(String context, String answer) {
        var evaluationRequest = new EvaluationRequest(List.of(new Document(context)), answer);
        return factCheckingEvaluator
                .evaluate(evaluationRequest)
                .isPass();
    }
}
