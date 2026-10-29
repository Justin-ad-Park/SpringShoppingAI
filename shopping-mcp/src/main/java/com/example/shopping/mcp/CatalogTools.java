package com.example.shopping.mcp;

import java.util.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import static com.example.shopping.mcp.ShoppingModels.*;

@Service
public class CatalogTools {
    private final MockShoppingStore store;
    public CatalogTools(MockShoppingStore store) { this.store = store; }

    @Tool(description = "범용 상품 검색. 장바구니나 무료배송 조건은 자동 적용하지 않습니다. query의 가격 범위는 양 끝 포함이며 minPrice 생략은 0원, maxPrice 생략은 상한 없음입니다. category는 식품/음료/생활, keyword는 상품명 검색입니다. 제외 상품과 배송그룹은 명시된 경우에만 필터링합니다. 재고 필터 기본 true, 정렬 기본 PRICE_ASC, limit 기본 20(최대 50).")
    public List<Product> searchProducts(SearchQuery query) {
        if (query == null) throw new IllegalArgumentException("검색 조건이 필요합니다.");
        long min = query.minPrice() == null ? 0 : query.minPrice();
        Long max = query.maxPrice();
        if (min < 0 || (max != null && max < 0)) throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
        if (query.limit() != null && (query.limit() < 1 || query.limit() > 50))
            throw new IllegalArgumentException("limit은 1~50입니다.");
        if (query.category() != null && !query.category().isBlank()
                && !List.of("식품", "음료", "생활").contains(query.category()))
            throw new IllegalArgumentException("지원하지 않는 카테고리");
        Set<String> excluded = query.excludeProductIds() == null ? Set.of() : new HashSet<>(query.excludeProductIds());
        List<String> groups = query.shippingGroups() == null ? List.of() : query.shippingGroups();
        String keyword = query.keyword() == null ? "" : query.keyword().strip().toLowerCase(Locale.ROOT);
        Comparator<Product> order = Comparator.comparingLong(Product::price);
        if (query.sort() == Sort.PRICE_DESC) order = order.reversed();
        return store.products().stream()
            .filter(p -> Boolean.FALSE.equals(query.inStockOnly()) || p.inStock())
            .filter(p -> p.price() >= min && (max == null || p.price() <= max))
            .filter(p -> query.category() == null || query.category().isBlank() || p.category().equals(query.category()))
            .filter(p -> keyword.isEmpty() || p.name().toLowerCase(Locale.ROOT).contains(keyword))
            .filter(p -> !excluded.contains(p.id()))
            .filter(p -> groups.isEmpty() || groups.contains(p.shippingGroup()))
            .sorted(order.thenComparing(Product::id)).limit(query.limit() == null ? 20 : query.limit()).toList();
    }
}
