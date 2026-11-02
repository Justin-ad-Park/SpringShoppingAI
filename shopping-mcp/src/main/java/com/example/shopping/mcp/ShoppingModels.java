package com.example.shopping.mcp;

import java.util.List;
import org.springframework.ai.tool.annotation.ToolParam;

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
    // 검색 필드는 모두 생략 가능하다. 2.0의 MCP 입력 검증에도 같은 계약을 노출한다.
    public record SearchQuery(
            @ToolParam(required = false) String keyword,
            @ToolParam(required = false) Long minPrice,
            @ToolParam(required = false) Long maxPrice,
            @ToolParam(required = false) String category,
            @ToolParam(required = false) List<String> excludeProductIds,
            @ToolParam(required = false) List<String> shippingGroups,
            @ToolParam(required = false) Boolean inStockOnly,
            @ToolParam(required = false) Sort sort,
            @ToolParam(required = false) Integer limit) {}
}
