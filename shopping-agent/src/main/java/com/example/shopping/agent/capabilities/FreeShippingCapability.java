package com.example.shopping.agent.capabilities;

import java.util.*;
import org.springframework.stereotype.Component;
import com.example.shopping.agent.ChatDtos.*;
import static com.example.shopping.agent.ports.ShoppingContracts.*;

/** Fixed workflow for arithmetic and eligibility; general product search remains reusable. */
@Component
public class FreeShippingCapability implements Capability {
    private final FreeShippingPolicy policy;
    public FreeShippingCapability(FreeShippingPolicy policy) { this.policy = policy; }
    public String id() { return "cart.freeShippingRecommendation"; }
    public String description() { return "현재 장바구니의 무료배송을 채울 추가 상품 추천. 이 데모의 무료배송 상품 추천은 이 업무를 의미. 후속 카테고리/예산 변경 시 목적 유지."; }
    public boolean searchTask() { return true; }
    public Result execute(Context context) {
        Cart cart = context.cart().cart();
        Policy shipping = context.cart().policy();
        if (cart.items().isEmpty()) return Result.info("장바구니가 비어 있습니다. 먼저 상품을 담아 주세요.", Map.of());
        Quote base = context.cart().quote(List.of());
        if (!base.valid()) throw new IllegalStateException("장바구니 견적 실패");
        if (base.shippingFee() == 0) return Result.info("이미 무료배송 조건을 충족했습니다. 추가 구매가 필요하지 않습니다.", Map.of("subtotal", base.subtotal()));
        var criteria = context.task().constraints();
        var bounds = policy.bounds(base.subtotal(), shipping.threshold(), criteria);
        Map<String,Object> details = Map.of("subtotal", base.subtotal(), "remaining", bounds.minPrice(),
            "minPrice", bounds.minPrice(), "maxPrice", bounds.maxPrice(),
            "budgetSource", criteria.maxPrice() == null ? "DEFAULT" : "USER");
        if (bounds.maxPrice() < bounds.minPrice()) return new Result(Outcome.NO_RESULTS,
            "무료배송까지 %,d원 부족하지만 추가 예산은 %,d원입니다. 조건에 맞는 단일 상품이 없습니다."
                .formatted(bounds.minPrice(), bounds.maxPrice()), List.of(), details, "WORKFLOW");
        List<String> excluded = cart.items().stream().map(i -> i.product().id()).toList();
        ProductQuery query = new ProductQuery(criteria.keyword(), bounds.minPrice(), bounds.maxPrice(),
            criteria.category(), excluded, List.of(cart.shippingGroup()), true, "PRICE_ASC", 20);
        List<Product> candidates = context.catalog().search(query);
        List<Card> cards = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Product product : candidates) {
            if (cards.size() == 3) break;
            // Defence in depth: validate the task contract even if a remote search is misconfigured.
            if (!seen.add(product.id()) || excluded.contains(product.id()) || !product.inStock()
                    || !cart.shippingGroup().equals(product.shippingGroup())
                    || product.price() < bounds.minPrice() || product.price() > bounds.maxPrice()
                    || (criteria.category() != null && !criteria.category().equals(product.category()))) continue;
            Quote quote = context.cart().quote(List.of(product.id()));
            if (quote.valid() && quote.shippingFee() == 0
                    && quote.subtotal() == Math.addExact(base.subtotal(), product.price())) {
                cards.add(new Card(product.id(), product.name(), product.price(), product.category(), product.emoji(),
                    new ShippingSummary(quote.subtotal(), quote.shippingFee())));
            }
        }
        String message = cards.isEmpty() ? "지정한 조건에 맞는 단일 추가 상품이 없습니다. 예산이나 카테고리 조건을 변경할 수 있습니다."
            : "장바구니 합계 %,d원, 무료배송까지 %,d원 부족합니다. %,d~%,d원 범위에서 찾았습니다. 아래 상품 중 하나를 1개 추가하면 무료배송입니다. 절약되는 배송비는 %,d원입니다."
                .formatted(base.subtotal(), bounds.minPrice(), bounds.minPrice(), bounds.maxPrice(), base.shippingFee());
        return new Result(cards.isEmpty() ? Outcome.NO_RESULTS : Outcome.SUCCESS, message, List.copyOf(cards), details, "WORKFLOW");
    }
}
