package com.example.ailearning.l06decisions;

import com.example.ailearning.l06decisions.domain.ProductCatalog;
import com.example.ailearning.l06decisions.domain.ProductPriceDetail;
import com.example.ailearning.l06decisions.domain.ProductSearchCondition;
import org.springframework.ai.tool.annotation.Tool;
import java.util.List;

/** 요청에서 확정한 조건으로 조회합니다. 추천 목록이나 호출 결과를 필드에 저장하지 않습니다. */
public class BudgetProductTools {
    private final ProductCatalog productCatalog;
    private final ProductSearchCondition searchCondition;

    public BudgetProductTools(ProductCatalog productCatalog, ProductSearchCondition searchCondition) {
        this.productCatalog = productCatalog;
        this.searchCondition = searchCondition;
    }

    @Tool(description = "고객 요청에서 확정한 카테고리와 배송비 포함 예산에 맞는 상품을 조회합니다. 결과가 비면 조건에 맞는 상품이 없습니다.")
    public List<ProductPriceDetail> findProductsWithinBudget() {
        return productCatalog.search(searchCondition.category(), searchCondition.totalBudget())
                .stream()
                .map(product -> productCatalog.calculatePriceDetails(product.id()))
                .filter(detail -> detail.total() <= searchCondition.totalBudget())
                .toList();
    }
}
