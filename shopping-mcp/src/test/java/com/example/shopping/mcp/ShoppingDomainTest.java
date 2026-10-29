package com.example.shopping.mcp;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.example.shopping.mcp.ShoppingModels.*;

class ShoppingDomainTest {
    private final MockShoppingStore store = new MockShoppingStore();
    private final CatalogTools catalog = new CatalogTools(store);
    private final PricingTools pricing = new PricingTools(store);
    private SearchQuery query(Long min, Long max, String category, List<String> excluded, List<String> groups) {
        return new SearchQuery("", min, max, category, excluded, groups, true, Sort.PRICE_ASC, 20);
    }
    @Test void genericSearchHasNoCartOrImplicitShippingBudget() {
        assertThat(catalog.searchProducts(new SearchQuery("텀블러", null, null, null, null, null, null, null, null)))
            .extracting(Product::id).containsExactly("P009");
        assertThat(catalog.searchProducts(query(null, null, null, null, null)))
            .extracting(Product::id).contains("P001", "P002", "P006", "P008", "P009").doesNotContain("P007");
    }
    @Test void filtersAreExplicitAndInclusive() {
        assertThat(catalog.searchProducts(query(8000L, 10000L, null, List.of("P001", "P002"), List.of("MALL"))))
            .extracting(Product::id).containsExactly("P003", "P004");
        assertThat(catalog.searchProducts(query(29000L, 29000L, "생활", null, null)))
            .extracting(Product::id).containsExactly("P009");
        assertThat(catalog.searchProducts(query(30000L, 10000L, null, null, null))).isEmpty();
    }
    @Test void categoryStockAndDescendingSort() {
        var query = new SearchQuery("", null, null, "식품", List.of(), List.of(), false, Sort.PRICE_DESC, 2);
        assertThat(catalog.searchProducts(query)).extracting(Product::id).containsExactly("P001", "P002");
        assertThat(catalog.searchProducts(new SearchQuery("프로틴", null, null, null, null, null, false, null, null)))
            .extracting(Product::id).containsExactly("P007");
    }
    @Test void domainQuoteHasNoRecommendationBudgetAndDoesNotMutateCart() {
        assertThat(store.cart("standard").subtotal()).isEqualTo(42000);
        assertThat(pricing.quoteCart("standard", List.of("P003")).subtotal()).isEqualTo(50900);
        assertThat(pricing.quoteCart("standard", List.of("P003")).shippingFee()).isZero();
        assertThat(pricing.quoteCart("standard", List.of("P009")).valid()).isTrue();
        assertThat(pricing.quoteCart("standard", List.of("P008")).shippingFee()).isEqualTo(3000);
        assertThat(store.cart("standard").subtotal()).isEqualTo(42000);
    }
    @Test void domainQuoteRejectsUnavailableUnsupportedAndUnknownProducts() {
        assertThat(pricing.quoteCart("standard", List.of("P006")).valid()).isFalse();
        assertThat(pricing.quoteCart("standard", List.of("P007")).valid()).isFalse();
        assertThatThrownBy(() -> pricing.quoteCart("standard", List.of("invented"))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void quoteHandlesEmptyAndAlreadyFree() {
        assertThat(pricing.quoteCart("empty", List.of()).shippingFee()).isZero();
        assertThat(pricing.quoteCart("already-free", List.of()).shippingFee()).isZero();
    }
    @Test void rejectsInvalidFilters() {
        assertThatThrownBy(() -> catalog.searchProducts(query(-1L, null, null, null, null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.searchProducts(query(null, null, "unknown", null, null))).isInstanceOf(IllegalArgumentException.class);
    }
}
