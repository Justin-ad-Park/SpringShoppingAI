package com.example.shopping.tutor.lesson.l06tools.support;

import java.util.*;

/** fixture 이름으로 고정된 장바구니 합계를 선택한다. 실제 고객 데이터는 사용하지 않는다. */
public final class DemoCartRepository {
    private DemoCartRepository() {}

    public static long subtotal(String fixture) {
        return switch (fixture) {
            case "standard" -> 42000;
            case "already-free" -> 60000;
            case "empty" -> 0;
            case "no-match" -> 1000;
            default -> throw new IllegalArgumentException("지원하지 않는 fixture입니다.");
        };
    }
}
