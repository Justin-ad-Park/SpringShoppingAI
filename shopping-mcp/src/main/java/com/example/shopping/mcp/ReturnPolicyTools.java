package com.example.shopping.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** 데모 상품의 개봉 상태에 해당하는 반품 정책을 조회한다. */
public class ReturnPolicyTools {
    @Tool(description = "개봉 여부에 따른 데모 반품 정책과 출처 ID를 알려줍니다.")
    public ReturnPolicy getReturnPolicy(
            @ToolParam(description = "반품할 상품의 개봉 상태. SEALED는 미개봉, OPENED는 개봉") PackagingCondition packagingCondition) {
        return switch (packagingCondition) {
            case SEALED -> new ReturnPolicy("return-sealed", "미개봉 상품은 수령 후 7일 이내 반품할 수 있습니다.");
            case OPENED -> new ReturnPolicy("return-opened", "개봉한 상품은 단순 변심 반품이 불가합니다. 불량은 별도로 확인합니다.");
        };
    }

    public enum PackagingCondition { SEALED, OPENED }

    public record ReturnPolicy(String sourceId, String text) {}
}
