package com.example.ailearning.l13shopping.lesson11_agent;

import com.example.ailearning.l13shopping.domain.Product;
import com.example.ailearning.l13shopping.domain.ProductCatalog;
import com.example.ailearning.l13shopping.domain.ProductPriceDetail;
import com.example.ailearning.l13shopping.policy.PolicySearch;
import com.example.ailearning.l13shopping.policy.PolicySearchResult;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 상품과 반품 정책을 조회하는 로컬 도구를 제공한다. */
@Component
public class Lesson11ShoppingTools {
    private final ProductCatalog productCatalog;
    private final PolicySearch policySearch;

    public Lesson11ShoppingTools(ProductCatalog productCatalog, PolicySearch policySearch) {
        this.productCatalog = productCatalog;
        this.policySearch = policySearch;
    }

    @Tool(description = "조건에 맞는 상품 후보를 검색합니다. 가격 상한에는 배송비를 포함하지 않습니다.")
    public List<Product> searchProducts(
            @ToolParam(description = "상품 종류. 음료 또는 생활. 생략하면 전체 검색", required = false) String category,
            @ToolParam(description = "상품 한 개의 가격 상한(원). 생략하면 상한 없음", required = false) Integer maxProductPrice) {
        return productCatalog.search(category, maxProductPrice);
    }

    @Tool(description = "상품 ID로 상품가, 배송비, 배송비 포함 총액을 조회합니다.")
    public ProductPriceDetail calculateShipping(@ToolParam(description = "조회할 상품 ID") String productId) {
        return productCatalog.calculatePriceDetail(productId);
    }

    @Tool(description = "반품 기간과 조건을 검색합니다. 질문에 개봉 상태가 있으면 검색어에도 포함하세요.")
    public List<PolicySearchResult> searchReturnPolicy(@ToolParam(description = "검색할 반품 질문") String question) {
        return policySearch.searchReturnPolicies(question);
    }
}
