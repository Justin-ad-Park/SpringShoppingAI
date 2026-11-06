package com.example.ailearning.l13shopping;
import java.util.Set;

public record Intent(String task, String category, Integer totalBudget) {
    public void validate() {
        if (!Set.of("SHOPPING", "POLICY")
                .contains(task == null ? "" : task))
            throw new IllegalArgumentException("처리할 업무가 올바르지 않습니다.");
        if (category != null && !Set.of("음료", "생활")
                .contains(category))
            throw new IllegalArgumentException("카테고리가 올바르지 않습니다.");
        if (totalBudget != null && totalBudget < 0) throw new IllegalArgumentException("예산은 0 이상입니다.");
    }
}
