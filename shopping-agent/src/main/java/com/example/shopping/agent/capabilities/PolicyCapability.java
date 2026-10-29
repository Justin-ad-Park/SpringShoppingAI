package com.example.shopping.agent.capabilities;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PolicyCapability implements Capability {
    public String id() { return "shipping.policy"; }
    public String description() { return "무료배송 조건/배송 정책만 조회하는 독립 업무. 상품을 추천하지 않음."; }
    public boolean searchTask() { return false; }
    public Result execute(Context context) {
        var policy = context.cart().policy();
        return Result.info("무료배송 기준은 %,d원 이상이며 기본 배송비는 %,d원입니다. %s"
            .formatted(policy.threshold(), policy.shippingFee(), policy.basis()), Map.of("policy", policy));
    }
}
