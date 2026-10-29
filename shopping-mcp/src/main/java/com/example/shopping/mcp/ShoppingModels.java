package com.example.shopping.mcp;

import java.util.List;

/** MCP domain contracts. Recommendation policies do not belong in these records. */
public final class ShoppingModels {
    private ShoppingModels() {}
    public record Product(String id, String name, long price, String category,
                          String shippingGroup, boolean inStock, String emoji) {}
    public record CartItem(Product product, int quantity) {}
    public record Cart(String scenario, List<CartItem> items, long subtotal, String shippingGroup) {}
    public record Policy(long threshold, long shippingFee, String shippingGroup, String basis) {}
    public record CartQuote(boolean valid, long subtotal, long shippingFee, long total, String reason) {}
    public enum Sort { PRICE_ASC, PRICE_DESC }
    public record SearchQuery(String keyword, Long minPrice, Long maxPrice, String category,
                              List<String> excludeProductIds, List<String> shippingGroups,
                              Boolean inStockOnly, Sort sort, Integer limit) {}
}
