package com.example.shopping.tutor.lesson.l06tools;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;
import com.example.shopping.tutor.lesson.l06tools.support.DemoProductCatalog;
import com.example.shopping.tutor.lesson.l06tools.support.Product;

import org.springframework.ai.tool.annotation.Tool;

import java.util.*;

/** 모델에는 이 메소드의 설명과 인자 스키마가 공개된다. 데이터 조회는 Java가 실행한다. */
public final class ProductSearchTools {
    @Tool(description = "가격 상한(원)과 카테고리(식품/음료/생활)로 재고 있는 학습 상품을 검색합니다.")
    public List<Product> searchProducts(String category, long maxPrice) {
        return DemoProductCatalog.search(new SearchCriteria(null, category, maxPrice));
    }
}
