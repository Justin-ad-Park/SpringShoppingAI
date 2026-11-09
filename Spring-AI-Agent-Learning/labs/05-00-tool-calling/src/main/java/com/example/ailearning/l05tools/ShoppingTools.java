package com.example.ailearning.l05tools;

import com.example.ailearning.l05tools.domain.Product;
import com.example.ailearning.l05tools.domain.ProductCatalog;
import com.example.ailearning.l05tools.domain.ProductPriceDetails;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ShoppingTools {
    private final ProductCatalog productCatalog;

    public ShoppingTools(ProductCatalog productCatalog) { this.productCatalog = productCatalog; }

    @Tool(description = "가상 상품을 검색합니다. category는 음료/생활, maxPrice는 상품 가격 상한입니다. 생략하면 해당 조건은 적용하지 않습니다.")
    public List<Product> searchProducts(
            @ToolParam(required = false) String category,
            @ToolParam(required = false) Integer maxPrice) {
        return productCatalog.search(category, maxPrice);
    }

    @Tool(description = "존재하는 상품 ID의 국내 기본 배송비와 총액을 조회합니다. 가격을 직접 입력하지 말고 검색 결과의 상품 ID를 사용하세요.")
    public ProductPriceDetails calculateShipping(String productId) { return productCatalog.calculatePriceDetails(productId); }
}
