package com.example.shopping.tutor.lesson.l06tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shopping.tutor.lesson.l06tools.support.Product;

import org.junit.jupiter.api.Test;

import java.util.List;

/** 먼저 Java 검색 결과를 이해한 뒤 AI와 연결한다. API 키는 필요 없다. */
class ProductSearchToolsTest {
    @Test
    void 가격이_만원_이하인_음료를_찾는다() {
        // 준비: 준비된 상품 목록에서 검색하는 메소드를 만든다.
        ProductSearchTools tools = new ProductSearchTools();

        // 실행: 만원 이하의 음료를 찾는다. P004 허브티는 9,900원이다.
        List<Product> products = tools.searchProducts("음료", 10000);

        // 확인: 커피 P005는 12,500원이므로 이 결과에는 없어야 한다.
        assertThat(products).extracting(Product::id).containsExactly("P004");
    }

    @Test
    void 가격을_만오천원으로_올리면_커피도_나온다() {
        ProductSearchTools tools = new ProductSearchTools();

        List<Product> products = tools.searchProducts("음료", 15000);

        assertThat(products).extracting(Product::id).containsExactly("P004", "P005");
    }

    @Test
    void 조건에_맞는_상품이_없으면_빈_목록을_받는다() {
        ProductSearchTools tools = new ProductSearchTools();

        List<Product> products = tools.searchProducts("음료", 1000);

        assertThat(products).isEmpty();
    }
}
