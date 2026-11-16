package com.example.ailearning.l14jev.domain;

/** 모델의 판단값을 서비스 처리 기준으로 변환합니다. 수치는 이 실습의 정책이며 Jev 기본값이 아닙니다. */
public final class SupportRoutingPolicy {
    private static final double MIN_CONFIDENCE = 0.7;
    private static final double HIGH_PRIORITY_SCORE = 1.5;
    private static final double NORMAL_PRIORITY_SCORE = 0.5;
    private static final double HUMAN_SUPPORT_YES_PROBABILITY = 0.8;
    private static final double HUMAN_SUPPORT_NO_PROBABILITY = 0.2;

    private SupportRoutingPolicy() {}

    public static boolean requiresIntentClarification(ShoppingIntent intent, double confidence) {
        return intent == ShoppingIntent.OTHER || confidence < MIN_CONFIDENCE;
    }

    public static SupportPriority determinePriority(double score, double confidence) {
        if (confidence < MIN_CONFIDENCE) return SupportPriority.REVIEW;
        if (score >= HIGH_PRIORITY_SCORE) return SupportPriority.HIGH;
        if (score >= NORMAL_PRIORITY_SCORE) return SupportPriority.NORMAL;
        return SupportPriority.LOW;
    }

    public static HumanSupportAction determineHumanSupportAction(double yesProbability) {
        if (yesProbability >= HUMAN_SUPPORT_YES_PROBABILITY) return HumanSupportAction.OFFER_HUMAN_SUPPORT;
        if (yesProbability <= HUMAN_SUPPORT_NO_PROBABILITY) return HumanSupportAction.CONTINUE_CHAT;
        return HumanSupportAction.ASK_CLARIFICATION;
    }
}
