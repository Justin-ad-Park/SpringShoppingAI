package com.example.ailearning.l08advisors.lesson08_memoryorder;

import org.springframework.ai.tool.annotation.Tool;

/** 08-8 전용: 상품명 인자를 포함한 도구 메시지를 관찰한다. 실습에서는 모든 상품에 고정 가격을 반환한다. */
public class TeaPriceTools {
    @Tool(description = "상품의 가격을 원 단위로 조회합니다.")
    public int getTeaPrice(String productName) {
        return 12000;
    }
}
