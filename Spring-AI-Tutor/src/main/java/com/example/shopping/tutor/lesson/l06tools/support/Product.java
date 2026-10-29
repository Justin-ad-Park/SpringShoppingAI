package com.example.shopping.tutor.lesson.l06tools.support;

import java.util.*;

/** 실제 판매 정보가 아닌 수업용 상품이다. 금액은 원 단위 정수다. */
public record Product(
        String id, String name, long price, String category, String group, boolean inStock) {}
