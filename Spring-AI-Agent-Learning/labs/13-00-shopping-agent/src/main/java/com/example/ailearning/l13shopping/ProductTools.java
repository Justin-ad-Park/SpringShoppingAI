package com.example.ailearning.l13shopping;

import com.example.ailearning.l13shopping.domain.Intent;
import com.example.ailearning.l13shopping.domain.Product;
import com.example.ailearning.l13shopping.domain.ProductCatalog;
import com.example.ailearning.l13shopping.domain.ProductPriceDetails;
import com.example.ailearning.l13shopping.support.RunState;
import org.springframework.ai.tool.annotation.Tool;
import java.util.List;

public class ProductTools {
    private final ProductCatalog catalog;
    private final Intent intent;
    private final TurnFacts facts;
    private final RunState run;
    public ProductTools(ProductCatalog catalog, Intent intent, TurnFacts facts, RunState run) {
        this.catalog = catalog; this.intent = intent; this.facts = facts; this.run = run;
    }
    @Tool(description = "Java가 확정한 카테고리와 예산으로 상품을 검색합니다. 총액은 배송비를 별도로 확인해야 합니다.")
    public List<Product> searchProducts() {
        run.startTool("searchProducts", intent.toString());
        List<Product> products = catalog.search(intent.category(), intent.totalBudget());
        run.check(); facts.productSearched = true; facts.lastProducts = products;
        run.recordToolResult("searchProducts", products);
        return products;
    }
    @Tool(description = "상품 ID로 실제 상품 가격과 배송비 포함 총액을 확인합니다.")
    public ProductPriceDetails calculateShipping(String productId) {
        run.startTool("calculateShipping", productId);
        ProductPriceDetails offer = catalog.calculatePriceDetails(productId);
        run.check(); facts.offers.put(productId, offer);
        run.recordToolResult("calculateShipping", offer);
        return offer;
    }
}
