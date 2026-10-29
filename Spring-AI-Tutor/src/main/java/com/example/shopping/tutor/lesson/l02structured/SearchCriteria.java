package com.example.shopping.tutor.lesson.l02structured;

import java.util.*;

/** 모델이 반환한 JSON도 이 생성자에서 업무 규칙을 검증한다. */
public record SearchCriteria(String keyword, String category, Long maxPrice) {
    public SearchCriteria {
        if (maxPrice != null && maxPrice < 0)
            throw new IllegalArgumentException("예산은 0원 이상이어야 합니다.");
        if (category != null && !List.of("식품", "음료", "생활").contains(category))
            throw new IllegalArgumentException("지원하지 않는 카테고리입니다.");
    }
}
