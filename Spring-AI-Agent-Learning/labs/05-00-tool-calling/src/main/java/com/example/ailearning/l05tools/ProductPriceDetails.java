package com.example.ailearning.l05tools;

/** 배송비와 총액은 모델이 아닌 Java 코드가 계산한다. */
public record ProductPriceDetails(Product product, int shippingFee, int total) {}
