package com.example.shopping.agent.capabilities;

import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class BenefitsCapability implements Capability {
    public String id() { return "benefits.summary"; }
    public String description() { return "쿠폰·적립금 보유 내역 조회. coupons/points로 대상 지정. 검색 상태를 바꾸지 않는 독립 업무."; }
    public boolean searchTask() { return false; }
    public Result execute(Context context) {
        Map<String,Object> details = new LinkedHashMap<>();
        boolean both = !context.decision().coupons() && !context.decision().points();
        if (both || context.decision().coupons()) details.put("coupons", context.benefits().coupons());
        if (both || context.decision().points()) details.put("points", context.benefits().points());
        return Result.info("보유 혜택을 조회했습니다. 아래 상세 결과를 확인하세요. 쿠폰과 적립금은 자동 사용하지 않습니다.", details);
    }
}
