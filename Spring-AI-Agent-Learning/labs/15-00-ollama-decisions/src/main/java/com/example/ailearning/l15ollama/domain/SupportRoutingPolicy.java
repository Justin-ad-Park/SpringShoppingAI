package com.example.ailearning.l15ollama.domain;

/** 모델의 판단값을 서비스 처리 기준으로 바꾼다. 임계값은 Ollama 기본값이 아닌 실습 정책이다. */
public final class SupportRoutingPolicy {
    private static final double MIN_CONFIDENCE = 0.7;
    private SupportRoutingPolicy() {}

    public static boolean requiresIntentClarification(ShoppingIntent intent, Double confidence) {
        return intent == ShoppingIntent.OTHER || confidence == null || confidence < MIN_CONFIDENCE;
    }

    public static SupportPriority determinePriority(double score, Double confidence) {
        if (confidence == null || confidence < MIN_CONFIDENCE) return SupportPriority.REVIEW;
        if (score >= 1.5) return SupportPriority.HIGH;
        if (score >= 0.5) return SupportPriority.NORMAL;
        return SupportPriority.LOW;
    }

    public static HumanSupportAction determineHumanSupportAction(double yesProbability) {
        if (yesProbability >= 0.8) return HumanSupportAction.OFFER_HUMAN_SUPPORT;
        if (yesProbability <= 0.2) return HumanSupportAction.CONTINUE_CHAT;
        return HumanSupportAction.ASK_CLARIFICATION;
    }
}
