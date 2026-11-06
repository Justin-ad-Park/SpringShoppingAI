package com.example.ailearning.l06decisions;

import com.example.ailearning.l06decisions.support.RunState;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import java.util.List;

/** 요청마다 새로 생성한다. 관찰 기록과 한도가 다른 요청에 섞이지 않는다. */
public class TracedShoppingTools {
    private final ProductCatalog catalog;
    private final RunState run;
    private final GiftCriteria fixedCriteria;

    public TracedShoppingTools(ProductCatalog catalog, RunState run, GiftCriteria fixedCriteria) {
        this.catalog = catalog;
        this.run = run;
        this.fixedCriteria = fixedCriteria;
    }

    @Tool(description = "가상 상품 검색. category는 음료/생활, maxPrice는 상품 가격 상한입니다. 총액에는 배송비를 별도로 확인해야 합니다.")
    public List<Product> searchProducts(@ToolParam(required = false) String category,
            @ToolParam(required = false) Integer maxPrice) {
        // 혼합 방식에서는 Java가 추출·검증한 조건을 모델이 완화할 수 없다.
        if (fixedCriteria != null) {
            category = fixedCriteria.category();
            maxPrice = fixedCriteria.totalBudget();
        }
        run.startTool("searchProducts", "category=" + category + ", maxPrice=" + maxPrice);
        List<Product> products = catalog.search(category, maxPrice);
        run.recordToolResult("searchProducts", products);
        return products;
    }

    @Tool(description = "존재하는 상품 ID의 국내 기본 배송비와 총액 조회. 검색한 상품 ID를 사용하세요.")
    public Offer calculateShipping(String productId) {
        run.startTool("calculateShipping", productId);
        Offer offer = catalog.calculatePriceDetails(productId);
        run.recordQuote(offer);
        run.recordToolResult("calculateShipping", offer);
        return offer;
    }
}
