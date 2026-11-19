package com.example.ailearning.l13limited.tools;

import com.example.ailearning.l13limited.domain.Intent;
import com.example.ailearning.l13limited.domain.Product;
import com.example.ailearning.l13limited.domain.ProductCatalog;
import com.example.ailearning.l13limited.domain.ProductPriceDetail;
import com.example.ailearning.l13limited.support.RunState;
import com.example.ailearning.l13limited.workflow.CounselToolResults;
import org.springframework.ai.tool.annotation.Tool;
import java.util.List;

public class ProductTools {
    private final ProductCatalog productCatalog;
    private final Intent intent;
    private final CounselToolResults toolResults;
    private final RunState runState;

    public ProductTools(ProductCatalog productCatalog, Intent intent,
            CounselToolResults toolResults, RunState runState) {
        this.productCatalog = productCatalog;
        this.intent = intent;
        this.toolResults = toolResults;
        this.runState = runState;
    }

    @Tool(description = "Java가 확정한 카테고리와 예산으로 상품을 검색합니다. 총액은 배송비를 별도로 확인해야 합니다.")
    public List<Product> searchProducts() {
        runState.beginToolExecution("searchProducts", intent.toString());
        List<Product> foundProducts = productCatalog.search(intent.category(), intent.totalBudget());

        runState.check();
        toolResults.recordProductSearch(foundProducts);
        runState.recordToolResult("searchProducts", foundProducts);
        return foundProducts;
    }

    @Tool(description = "상품 ID로 실제 상품 가격과 배송비 포함 총액을 확인합니다.")
    public ProductPriceDetail calculateShipping(String productId) {
        runState.beginToolExecution("calculateShipping", productId);
        ProductPriceDetail priceDetail = productCatalog.calculatePriceDetail(productId);

        runState.check();
        toolResults.recordPriceLookup(productId, priceDetail);
        runState.recordToolResult("calculateShipping", priceDetail);
        return priceDetail;
    }
}
