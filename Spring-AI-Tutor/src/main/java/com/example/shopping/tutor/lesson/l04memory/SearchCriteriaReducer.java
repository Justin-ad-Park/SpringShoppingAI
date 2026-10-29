package com.example.shopping.tutor.lesson.l04memory;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;

/** null은 미입력, 명시적인 해제 문구는 기존 조건 삭제를 뜻한다. */
public final class SearchCriteriaReducer {
    private SearchCriteriaReducer() {}

    public static SearchCriteria merge(
            SearchCriteria previous, SearchCriteria proposed, String question) {
        String keyword = previous.keyword();
        String category = previous.category();
        Long maxPrice = previous.maxPrice();
        if (proposed.keyword() != null) {
            keyword = proposed.keyword();
        }
        if (proposed.category() != null) {
            category = proposed.category();
        }
        if (proposed.maxPrice() != null) {
            maxPrice = proposed.maxPrice();
        }
        // 새 조건이 없다는 것과 기존 조건을 지우라는 것은 서로 다른 사용자 의도다.
        if (question.contains("카테고리 해제")) {
            category = null;
        }
        if (question.contains("예산 해제")) {
            maxPrice = null;
        }
        return new SearchCriteria(keyword, category, maxPrice);
    }
}
