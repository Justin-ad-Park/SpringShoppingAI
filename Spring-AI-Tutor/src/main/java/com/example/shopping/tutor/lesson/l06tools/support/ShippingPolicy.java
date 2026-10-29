package com.example.shopping.tutor.lesson.l06tools.support;

import java.util.*;

/** 배송비 계산은 언어 모델이 아니라 결정적인 Java 규칙으로 처리한다. */
public final class ShippingPolicy {
    private ShippingPolicy() {}

    public static final long FREE_SHIPPING_THRESHOLD = 50000;

    public static long shippingFee(long subtotal) {
        if (subtotal < 0) throw new IllegalArgumentException("음수 합계");
        return subtotal == 0 || subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : 3000;
    }
}
