package com.example.ailearning.l06decisions.domain;

import java.util.Set;

/** 상품 검색에 사용할 카테고리와 배송비 포함 총예산을 담는다. */
public record ProductSearchCondition(String category, Integer totalBudget) {
    public void validate() {
        if (category != null && !Set.of("음료", "생활")
                .contains(category))
            throw new IllegalArgumentException("카테고리가 올바르지 않습니다.");
        if (totalBudget != null && totalBudget < 0)
            throw new IllegalArgumentException("총예산은 0 이상이어야 합니다.");
    }
}
