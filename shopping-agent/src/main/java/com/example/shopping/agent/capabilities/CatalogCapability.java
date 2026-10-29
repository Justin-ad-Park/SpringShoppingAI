package com.example.shopping.agent.capabilities;

import java.util.*;
import org.springframework.stereotype.Component;
import com.example.shopping.agent.ChatDtos.Card;
import static com.example.shopping.agent.ports.ShoppingContracts.*;

@Component
public class CatalogCapability implements Capability {
    private final CatalogSearchStrategy strategy;
    public CatalogCapability(CatalogSearchStrategy strategy) { this.strategy = strategy; }
    public String id() { return "catalog.search"; }
    public String description() { return "일반 상품명·카테고리·가격 검색. 무료배송 조건은 자동 적용하지 않음. 무료배송 상관없이/조건 빼고 검색 요청도 이 업무를 사용."; }
    public boolean searchTask() { return true; }
    public Result execute(Context context) {
        var criteria = context.task().constraints();
        ProductQuery query = new ProductQuery(criteria.keyword(), 0L, criteria.maxPrice(), criteria.category(),
            List.of(), List.of(), true, "PRICE_ASC", 20);
        List<Product> products;
        String engine = strategy.name();
        try { products = strategy.search(context.message(), query, context.catalog()); }
        catch (CatalogSearchStrategy.InvalidPlan e) {
            // Only a verified planning/output failure triggers one safe read-only fallback.
            products = context.catalog().search(query);
            engine = "WORKFLOW_FALLBACK";
        }
        List<Card> cards = products.stream().limit(5)
            .map(p -> new Card(p.id(), p.name(), p.price(), p.category(), p.emoji(), null)).toList();
        return new Result(cards.isEmpty() ? Outcome.NO_RESULTS : Outcome.SUCCESS,
            cards.isEmpty() ? "지정한 검색 조건에 맞는 상품이 없습니다." : "지정한 조건으로 상품을 찾았습니다. 일반 검색에는 장바구니 무료배송 조건을 적용하지 않습니다.",
            cards, Map.of("query", query), engine);
    }
}
