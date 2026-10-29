package com.example.shopping.tutor.lesson.l06tools.support;

import java.util.*;

/** 상품 하나씩 추가했을 때의 가상 견적이다. 원래 장바구니는 변경하지 않는다. */
public final class ShippingQuoteCalculator {
    private ShippingQuoteCalculator() {}

    public static Map<String, Object> quote(String fixture, List<String> productIds) {
        long subtotal = DemoCartRepository.subtotal(fixture);
        for (String id : productIds) {
            Product product = DemoProductCatalog.findProduct(id);
            if (!product.inStock() || !product.group().equals("MALL"))
                throw new IllegalArgumentException("품절 또는 다른 배송그룹입니다.");
            subtotal = Math.addExact(subtotal, product.price());
        }
        return Map.of(
                "projectedSubtotal", subtotal, "shippingFee", ShippingPolicy.shippingFee(subtotal));
    }
}
