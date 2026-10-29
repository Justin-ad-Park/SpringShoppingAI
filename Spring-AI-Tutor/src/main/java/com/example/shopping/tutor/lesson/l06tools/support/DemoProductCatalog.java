package com.example.shopping.tutor.lesson.l06tools.support;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;

import java.util.*;

/** 모든 단원에서 같은 가상 상품 사실을 조회한다. 외부 DB나 모델 호출은 없다. */
public final class DemoProductCatalog {
    private DemoProductCatalog() {}

    private static final List<Product> PRODUCTS =
            List.of(
                    new Product("P001", "데일리 그래놀라", 18000, "식품", "MALL", true),
                    new Product("P002", "오트 밀크 6팩", 12000, "식품", "MALL", true),
                    new Product("P003", "견과 스낵 세트", 8900, "식품", "MALL", true),
                    new Product("P004", "허브티 컬렉션", 9900, "음료", "MALL", true),
                    new Product("P005", "드립 커피 10개입", 12500, "음료", "MALL", true),
                    new Product("P006", "파트너 꿀 세트", 8500, "식품", "PARTNER", true),
                    new Product("P007", "프로틴 바 세트", 9000, "식품", "MALL", false),
                    new Product("P008", "미니 잼", 4500, "식품", "MALL", true),
                    new Product("P009", "텀블러", 29000, "생활", "MALL", true));

    public static List<Product> products() {
        return PRODUCTS;
    }

    public static Product findProduct(String id) {
        return PRODUCTS.stream()
                .filter(product -> product.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
    }

    public static List<Product> search(SearchCriteria criteria) {
        return PRODUCTS.stream()
                .filter(Product::inStock)
                .filter(
                        product ->
                                criteria.category() == null
                                        || product.category().equals(criteria.category()))
                .filter(
                        product ->
                                criteria.maxPrice() == null
                                        || product.price() <= criteria.maxPrice())
                .filter(
                        product ->
                                criteria.keyword() == null
                                        || product.name().contains(criteria.keyword()))
                .sorted(Comparator.comparingLong(Product::price))
                .toList();
    }
}
