package com.example.ailearning.l11mcp.domain;

import java.util.List;
import java.util.Map;

/** maxPrice는 상품 하나의 상한, totalBudget은 배송비를 포함한 장바구니 전체 상한이다. */
public record SearchConditions(String category, Integer maxPrice, Long totalBudget) {
    public void validate() {
        if (category != null && !List.of("식품", "음료", "생활").contains(category)) {
            throw new IllegalArgumentException("지원하지 않는 카테고리입니다.");
        }
        if ((maxPrice != null && maxPrice < 0) || (totalBudget != null && totalBudget < 0)) {
            throw new IllegalArgumentException("가격과 예산은 0 이상이어야 합니다.");
        }
    }

    public boolean isComplete() {
        return category != null && maxPrice != null;
    }

    public Map<String, Object> searchArguments() {
        return Map.of("query", Map.of("category", category, "maxPrice", maxPrice,
                "shippingGroups", List.of("MALL"), "inStockOnly", true, "limit", 20));
    }
}
