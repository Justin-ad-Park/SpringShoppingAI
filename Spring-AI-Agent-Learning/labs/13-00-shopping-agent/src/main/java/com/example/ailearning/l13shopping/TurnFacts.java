package com.example.ailearning.l13shopping;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 모델의 말이 아닌 이번 요청에서 실제 조회한 사실만 보관한다. */
public class TurnFacts {
    final Map<String, ProductPriceDetails> offers = new LinkedHashMap<>();
    final Map<String, Source> sources = new LinkedHashMap<>();
    boolean productSearched, policySearched;
    List<Product> lastProducts = List.of();
}
