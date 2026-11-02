package com.example.ailearning.l08advisors;

import org.springframework.ai.tool.annotation.Tool;

public class PriceTools {
    @Tool(description = "가상 허브티 P01의 상품 가격만 조회합니다. 배송비는 포함하지 않습니다.")
    public int getTeaPrice() { return 12000; }
}
