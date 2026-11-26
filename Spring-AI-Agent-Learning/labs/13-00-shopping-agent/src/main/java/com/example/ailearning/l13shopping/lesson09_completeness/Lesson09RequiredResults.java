package com.example.ailearning.l13shopping.lesson09_completeness;

/** 고객이 이번 요청에서 원하는 두 결과 항목. 모델이 추출하며 실제 완료 여부와는 다르다. */
public record Lesson09RequiredResults(boolean productRecommendation, boolean policyGuidance) {
    public void validate() {
        if (!productRecommendation && !policyGuidance) {
            throw new IllegalArgumentException("상품 추천 또는 정책 안내가 필요한 요청을 사용하세요.");
        }
    }
}
