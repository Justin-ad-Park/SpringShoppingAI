package com.example.ailearning.l06decisions.domain;

/** 상품 한 개의 정보와 배송비, 배송비를 포함한 총액을 담는다. */
public record ProductPriceDetail(
        Product product,
        int shippingFee,
        int total) {}
