package com.example.ailearning.l06decisions;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import java.util.ArrayList;
import java.util.List;

/** 요청마다 새로 만든다. 조회한 금액이 다른 요청에 섞이지 않게 한다. */
public class ShoppingTools {
    private final ProductCatalog productCatalog;
    private final List<Offer> priceDetails = new ArrayList<>();

    public ShoppingTools(ProductCatalog productCatalog) {
        this.productCatalog = productCatalog;
    }

    @Tool(description = "상품 검색. category는 음료/생활, maxPrice는 상품 가격 상한. 배송비는 별도로 조회하세요.")
    public List<Product> searchProducts(@ToolParam(required = false) String category,
            @ToolParam(required = false) Integer maxPrice) {
        return productCatalog.search(category, maxPrice);
    }

    @Tool(description = "검색한 상품 ID의 배송비와 배송비 포함 총액을 조회합니다.")
    public Offer calculateShipping(String productId) {
        Offer offer = productCatalog.calculatePriceDetails(productId);
        priceDetails.add(offer);
        return offer;
    }

    /** 이번 요청에서 실제 조회한 금액을 반환한다. 최종 후보 검증에 사용한다. */
    public List<Offer> getPriceDetails() {
        return List.copyOf(priceDetails);
    }
}
