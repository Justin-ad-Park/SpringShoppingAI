package com.example.ailearning.l13shopping;

/** 배송비와 총액은 모델이 아닌 Java 코드가 계산한다. */
public record Offer(Product product, int shippingFee, int total) {}
