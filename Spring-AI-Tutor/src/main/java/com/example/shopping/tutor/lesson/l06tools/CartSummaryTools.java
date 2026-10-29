package com.example.shopping.tutor.lesson.l06tools;

import com.example.shopping.tutor.lesson.l06tools.support.DemoCartRepository;
import com.example.shopping.tutor.lesson.l06tools.support.ShippingPolicy;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

import java.util.*;

/** 고객 식별자는 모델 인자로 받지 않고 서버가 제공한 ToolContext에서 읽는다. */
public final class CartSummaryTools {
    private final String fixture;

    public CartSummaryTools(String fixture) {
        DemoCartRepository.subtotal(fixture);
        this.fixture = fixture;
    }

    @Tool(description = "현재 데모 장바구니의 합계, 무료배송 기준, 부족 금액을 조회합니다.")
    public Map<String, Object> getCartSummary(ToolContext context) {
        if (context.getContext().get("customerId") == null)
            throw new IllegalArgumentException("신뢰할 고객 context가 필요합니다.");
        long subtotal = DemoCartRepository.subtotal(fixture);
        return Map.of(
                "subtotal",
                subtotal,
                "threshold",
                50000,
                "missing",
                Math.max(0, 50000 - subtotal),
                "shippingFee",
                ShippingPolicy.shippingFee(subtotal));
    }
}
