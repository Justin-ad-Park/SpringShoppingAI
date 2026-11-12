package com.example.ailearning.l13shopping.domain;

/** 배송비와 총액은 모델이 아닌 Java 코드가 계산한다. */
public record ProductPriceDetail(Product product, int shippingFee, int total) {}
