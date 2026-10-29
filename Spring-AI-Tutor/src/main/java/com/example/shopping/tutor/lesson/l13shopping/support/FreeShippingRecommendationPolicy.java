package com.example.shopping.tutor.lesson.l13shopping.support;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;
import com.example.shopping.tutor.lesson.l06tools.support.*;

import java.util.*;

/** 추천 후보의 금액과 배송그룹은 Java가 제한한다. 모델의 설명이 후보를 늘릴 수 없다. */
public final class FreeShippingRecommendationPolicy {
    private FreeShippingRecommendationPolicy() {}

    public static long maxPrice(SearchCriteria criteria, long missing) {
        return criteria.maxPrice() == null ? missing + 7000 : criteria.maxPrice();
    }

    public static List<Product> candidates(
            SearchCriteria criteria, String fixture, long missing, long maxPrice) {
        var search = new SearchCriteria(criteria.keyword(), criteria.category(), maxPrice);
        return DemoProductCatalog.search(search).stream()
                .filter(product -> product.price() >= missing)
                .filter(product -> product.group().equals("MALL"))
                .filter(
                        product ->
                                !fixture.equals("standard")
                                        || !Set.of("P001", "P002").contains(product.id()))
                .toList();
    }
}
