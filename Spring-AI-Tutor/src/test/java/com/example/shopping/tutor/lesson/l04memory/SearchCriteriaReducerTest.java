package com.example.shopping.tutor.lesson.l04memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;

import org.junit.jupiter.api.Test;

/** 이전 조건 유지와 명시적인 해제는 별도로 확인한다. */
class SearchCriteriaReducerTest {
    @Test
    void clearAndKeepAreDifferent() {
        var old = new SearchCriteria(null, "음료", 30000L);
        var none = new SearchCriteria(null, null, null);
        assertThat(SearchCriteriaReducer.merge(old, none, "예산 해제"))
                .isEqualTo(new SearchCriteria(null, "음료", null));
        assertThat(SearchCriteriaReducer.merge(old, none, "계속 찾아줘")).isEqualTo(old);
    }
}
