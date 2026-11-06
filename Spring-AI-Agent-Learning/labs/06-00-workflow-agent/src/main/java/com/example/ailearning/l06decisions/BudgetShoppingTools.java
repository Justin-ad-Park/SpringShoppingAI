package com.example.ailearning.l06decisions;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import java.util.List;

/** 06-3에서 추가하는 도구. 검색 조건은 코드로 고정하고 조회 선택은 모델에 맡긴다. */
public class BudgetShoppingTools {
    private final ShoppingTools shoppingTools;
    private final ProductSelectionCriteria criteria;

    public BudgetShoppingTools(ProductCatalog productCatalog, ProductSelectionCriteria criteria) {
        this.shoppingTools = new ShoppingTools(productCatalog);
        this.criteria = criteria;
    }

    @Tool(description = "확정된 카테고리와 총예산 안에서 상품을 검색합니다. 배송비는 별도로 조회하세요.")
    public List<Product> searchProducts(@ToolParam(required = false) String category,
            @ToolParam(required = false) Integer maxPrice) {
        return shoppingTools.searchProducts(criteria.category(), criteria.totalBudget());
    }

    @Tool(description = "검색한 상품 ID의 배송비와 배송비 포함 총액을 조회합니다.")
    public ProductPriceDetails calculateShipping(String productId) {
        return shoppingTools.calculateShipping(productId);
    }

    public List<ProductPriceDetails> getPriceDetails() {
        return shoppingTools.getPriceDetails();
    }
}
