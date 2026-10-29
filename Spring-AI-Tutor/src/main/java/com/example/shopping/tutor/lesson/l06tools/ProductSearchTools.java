package com.example.shopping.tutor.lesson.l06tools;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;
import com.example.shopping.tutor.lesson.l06tools.support.DemoProductCatalog;
import com.example.shopping.tutor.lesson.l06tools.support.Product;

import org.springframework.ai.tool.annotation.Tool;

import java.util.*;

/** AI가 상품 검색을 요청하면 실행할 Java 메소드. 가격은 준비된 상품 목록에서 조회한다. */
public final class ProductSearchTools {
    @Tool(description = "가격 상한(원)과 카테고리(식품/음료/생활)로 재고 있는 학습 상품을 검색합니다.")
    public List<Product> searchProducts(String category, long maxPrice) {
        // 예: category="음료", maxPrice=10000이면 만원 이하인 음료를 찾는다.
        // 첫 번째 null은 특정 상품 이름으로는 제한하지 않는다는 뜻이다.
        SearchCriteria criteria = new SearchCriteria(null, category, maxPrice);
        return DemoProductCatalog.search(criteria);
    }
}
