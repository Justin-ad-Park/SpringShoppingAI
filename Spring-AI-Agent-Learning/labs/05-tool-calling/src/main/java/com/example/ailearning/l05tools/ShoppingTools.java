package com.example.ailearning.l05tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ShoppingTools {
    private final ProductCatalog catalog;

    public ShoppingTools(ProductCatalog catalog) { this.catalog = catalog; }

    @Tool(description = "가상 상품을 검색합니다. category는 음료/생활, maxPrice는 상품 가격 상한입니다. 생략하면 해당 조건은 적용하지 않습니다.")
    public List<Product> searchProducts(
            @ToolParam(required = false) String category,
            @ToolParam(required = false) Integer maxPrice) {
        return catalog.search(category, maxPrice);
    }

    @Tool(description = "존재하는 상품 ID의 국내 기본 배송비와 총액을 조회합니다. 가격을 직접 입력하지 말고 검색 결과의 상품 ID를 사용하세요.")
    public Offer calculateShipping(String productId) { return catalog.quote(productId); }
}
