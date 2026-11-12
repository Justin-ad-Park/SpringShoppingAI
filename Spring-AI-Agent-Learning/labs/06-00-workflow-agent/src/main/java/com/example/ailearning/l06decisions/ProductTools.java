package com.example.ailearning.l06decisions;

import com.example.ailearning.l06decisions.domain.Product;
import com.example.ailearning.l06decisions.domain.ProductCatalog;
import com.example.ailearning.l06decisions.domain.ProductPriceDetail;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import java.util.List;

/** 상품 검색과 배송비 조회를 모델에 제공하는 상태 없는 도구입니다. */
public class ProductTools {
    private final ProductCatalog productCatalog;

    public ProductTools(ProductCatalog productCatalog) {
        this.productCatalog = productCatalog;
    }

    @Tool(description = "카테고리와 상품 가격 상한으로 상품을 검색합니다. 가격에 배송비는 포함되지 않습니다.")
    public List<Product> searchProducts(@ToolParam(required = false) String category,
            @ToolParam(required = false) Integer maxPrice) {
        return productCatalog.search(category, maxPrice);
    }

    @Tool(description = "상품 한 개의 정보와 배송비, 배송비 포함 총액을 반환합니다.")
    public ProductPriceDetail calculateShipping(String productId) {
        return productCatalog.calculatePriceDetails(productId);
    }
}
