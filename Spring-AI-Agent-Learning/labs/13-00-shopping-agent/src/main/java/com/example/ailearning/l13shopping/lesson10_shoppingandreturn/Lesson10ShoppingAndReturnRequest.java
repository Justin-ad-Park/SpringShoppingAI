package com.example.ailearning.l13shopping.lesson10_shoppingandreturn;

/** 상품 추천과 반품 안내를 각각 실행하는 데 필요한 입력을 담는다. */
public record Lesson10ShoppingAndReturnRequest(String category, Integer totalBudget, ReturnCondition returnCondition) {
    boolean hasProductCriteria() {
        return ("음료".equals(category) || "생활".equals(category))
                && totalBudget != null && totalBudget >= 0;
    }

    public enum ReturnCondition {
        SEALED("미개봉 상품 반품 기간", "return-sealed"),
        OPENED("개봉한 상품 반품 조건", "return-opened");

        private final String searchQuery;
        private final String sourceId;

        ReturnCondition(String searchQuery, String sourceId) {
            this.searchQuery = searchQuery;
            this.sourceId = sourceId;
        }

        String searchQuery() { return searchQuery; }

        String sourceId() { return sourceId; }
    }
}
