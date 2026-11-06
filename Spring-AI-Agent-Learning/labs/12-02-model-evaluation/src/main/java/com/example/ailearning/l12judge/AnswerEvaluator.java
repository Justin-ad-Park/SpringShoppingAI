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
    private final RelevancyEvaluator relevance;
    private final FactCheckingEvaluator facts;

    public AnswerEvaluator(ChatClient.Builder builder) {
        relevance = new RelevancyEvaluator(builder.clone());
        facts = FactCheckingEvaluator.builder(builder.clone())
                .build();
    }

    public boolean relevance(String question, String context, String answer) {
        var request = new EvaluationRequest(question, List.of(new Document(context)), answer);
        return relevance
                .evaluate(request)
                .isPass();
    }

    public boolean facts(String context, String answer) {
        var request = new EvaluationRequest(List.of(new Document(context)), answer);
        return facts
                .evaluate(request)
                .isPass();
    }
}
