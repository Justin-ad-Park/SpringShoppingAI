package com.example.shopping.mcp;

import java.util.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class BenefitsTools {
    @Tool(description = "데모 고객 쿠폰 조회. 자동 적용하지 않습니다.")
    public Map<String, Object> getCoupons() {
        return Map.of("coupons", List.of(Map.of("name", "웰컴 할인 쿠폰", "discount", 2000, "minimumPurchase", 30000)), "autoApplied", false);
    }
    @Tool(description = "데모 고객 적립금 조회. 자동 사용하지 않습니다.")
    public Map<String, Object> getPoints() { return Map.of("balance", 3500, "currency", "KRW", "autoApplied", false); }
}
