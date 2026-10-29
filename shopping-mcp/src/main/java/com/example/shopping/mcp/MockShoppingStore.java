package com.example.shopping.mcp;

import java.util.List;
import org.springframework.stereotype.Repository;
import static com.example.shopping.mcp.ShoppingModels.*;

/** Immutable demo data; scenario selects a fixture, not a product-search policy. */
@Repository
public class MockShoppingStore {
    private final List<Product> products = List.of(
        new Product("P001", "데일리 그래놀라", 18000, "식품", "MALL", true, "🥣"),
        new Product("P002", "오트 밀크 6팩", 12000, "식품", "MALL", true, "🥛"),
        new Product("P003", "견과 스낵 세트", 8900, "식품", "MALL", true, "🥜"),
        new Product("P004", "허브티 컬렉션", 9900, "음료", "MALL", true, "🍵"),
        new Product("P005", "드립 커피 10개입", 12500, "음료", "MALL", true, "☕"),
        new Product("P006", "파트너 꿀 세트", 8500, "식품", "PARTNER", true, "🍯"),
        new Product("P007", "프로틴 바 세트", 9000, "식품", "MALL", false, "🍫"),
        new Product("P008", "미니 잼", 4500, "식품", "MALL", true, "🍓"),
        new Product("P009", "텀블러", 29000, "생활", "MALL", true, "🥤")
    );
    public List<Product> products() { return products; }
    public Product product(String id) {
        return products.stream().filter(p -> p.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품 ID"));
    }
    public Cart cart(String scenario) {
        List<CartItem> items = switch (scenario) {
            case "standard" -> List.of(new CartItem(product("P001"), 1), new CartItem(product("P002"), 2));
            case "already-free" -> List.of(new CartItem(product("P001"), 2), new CartItem(product("P002"), 2));
            case "empty" -> List.of();
            case "no-match" -> List.of(new CartItem(new Product("P010", "샘플 비스킷", 1000, "식품", "MALL", true, "🍪"), 1));
            default -> throw new IllegalArgumentException("지원하지 않는 scenario");
        };
        return new Cart(scenario, items,
            items.stream().mapToLong(i -> Math.multiplyExact(i.product().price(), i.quantity())).sum(), "MALL");
    }
}
