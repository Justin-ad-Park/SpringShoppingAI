package com.example.ailearning.l13shopping.workflow;

import com.example.ailearning.l13shopping.domain.Product;
import com.example.ailearning.l13shopping.domain.ProductPriceDetails;
import com.example.ailearning.l13shopping.policy.PolicySearchResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 한 상담 요청에서 도구가 실제로 조회한 결과를 모아 둔다. */
public final class CounselRequestFacts {
    private final Map<String, ProductPriceDetails> priceDetailsByProductId = new LinkedHashMap<>();
    private final Map<String, PolicySearchResult> policyResultsById = new LinkedHashMap<>();
    private boolean productSearchPerformed;
    private boolean policySearchPerformed;
    private List<Product> lastProductSearchResults = List.of();

    public void recordProductSearch(List<Product> searchResults) {
        productSearchPerformed = true;
        lastProductSearchResults = List.copyOf(searchResults);
    }

    public void recordPriceLookup(String productId, ProductPriceDetails priceDetails) {
        priceDetailsByProductId.put(productId, priceDetails);
    }

    public void recordPolicySearch(List<PolicySearchResult> searchResults) {
        policySearchPerformed = true;
        searchResults.forEach(result -> policyResultsById.put(result.id(), result));
    }

    public ProductPriceDetails priceDetailsFor(String productId) {
        return priceDetailsByProductId.get(productId);
    }

    public PolicySearchResult policyResultFor(String policyId) {
        return policyResultsById.get(policyId);
    }

    public boolean productSearchPerformed() {
        return productSearchPerformed;
    }

    public boolean policySearchPerformed() {
        return policySearchPerformed;
    }

    public List<Product> lastProductSearchResults() {
        return lastProductSearchResults;
    }

    public boolean hasPolicySearchResults() {
        return !policyResultsById.isEmpty();
    }
}
