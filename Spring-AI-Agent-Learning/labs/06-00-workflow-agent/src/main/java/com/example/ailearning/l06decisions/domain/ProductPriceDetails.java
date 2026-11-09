package com.example.ailearning.l06decisions.domain;

/** 상품과 배송비, 배송비를 포함한 총액을 담는다. */
public record ProductPriceDetails(
        Product product,
        int shippingFee,
        int total) {}
