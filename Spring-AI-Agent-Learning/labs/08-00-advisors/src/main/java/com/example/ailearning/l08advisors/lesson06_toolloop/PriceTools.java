package com.example.ailearning.l08advisors.lesson06_toolloop;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/** 08-6·08-7: 도구 루프를 관찰하기 위한 가상 허브티의 고정 가격 조회 도구다. */
@Component
public class PriceTools {
    @Tool(description = "가상 허브티 P01의 상품 가격만 조회합니다. 배송비는 포함하지 않습니다.")
    public int getTeaPrice() {
        return 12000;
    }
}
