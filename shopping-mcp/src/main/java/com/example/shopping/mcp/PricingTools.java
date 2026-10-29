package com.example.shopping.mcp;

import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import static com.example.shopping.mcp.ShoppingModels.*;

@Service
public class PricingTools {
    private final MockShoppingStore store;
    private final Policy policy = new Policy(50000, 3000, "MALL", "MALL 합산, 할인 미적용 상품 금액 기준. 쿠폰·적립금 자동 사용 없음.");
    public PricingTools(MockShoppingStore store) { this.store = store; }
    @Tool(description = "실제 배송 정책 조회. MALL 배송그룹 50,000원 이상 무료배송, 미만 3,000원. 추천용 예산 정책은 포함하지 않습니다.")
    public Policy getShippingPolicy() { return policy; }

    @Tool(description = "추가 상품을 각 1개씩 넣은 가상 장바구니의 합계와 배송비를 계산합니다. 실제 장바구니는 변경하지 않습니다. additionalProductIds는 빈 배열도 가능합니다. 추천 여부나 예산 상한은 판정하지 않습니다. Mock 견적은 같은 배송그룹만 지원하고 품절은 거부합니다.")
    public CartQuote quoteCart(String scenario, List<String> additionalProductIds) {
        if (additionalProductIds == null || additionalProductIds.size() > 20)
            throw new IllegalArgumentException("추가 상품 목록은 최대 20개입니다.");
        Cart cart = store.cart(scenario);
        long subtotal = cart.subtotal();
        for (String id : additionalProductIds) {
            Product p = store.product(id);
            if (!p.inStock() || !p.shippingGroup().equals(cart.shippingGroup()))
                return new CartQuote(false, subtotal, 0, subtotal, "품절 또는 별도 배송그룹 견적 미지원");
            subtotal = Math.addExact(subtotal, p.price());
        }
        long fee = subtotal >= policy.threshold() || (cart.items().isEmpty() && additionalProductIds.isEmpty()) ? 0 : policy.shippingFee();
        return new CartQuote(true, subtotal, fee, Math.addExact(subtotal, fee), "현재 Mock 가격·배송 정책 적용");
    }
}
