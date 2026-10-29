package com.example.shopping.agent.ports;

import java.util.List;

/** Typed domain boundary; no Spring AI/MCP types in the task contracts. */
public final class ShoppingContracts {
    private ShoppingContracts() {}
    public record Product(String id, String name, long price, String category, String shippingGroup, boolean inStock, String emoji) {}
    public record CartItem(Product product, int quantity) {}
    public record Cart(String scenario, List<CartItem> items, long subtotal, String shippingGroup) {}
    public record Policy(long threshold, long shippingFee, String shippingGroup, String basis) {}
    public record Quote(boolean valid, long subtotal, long shippingFee, long total, String reason) {}
    public record ProductQuery(String keyword, Long minPrice, Long maxPrice, String category,
                               List<String> excludeProductIds, List<String> shippingGroups,
                               boolean inStockOnly, String sort, int limit) {}
    public interface Catalog { List<Product> search(ProductQuery query); }
    public interface CartPricing {
        Cart cart();
        Policy policy();
        Quote quote(List<String> additionalProductIds);
    }
    public interface Benefits {
        java.util.Map<String,Object> coupons();
        java.util.Map<String,Object> points();
    }
}
