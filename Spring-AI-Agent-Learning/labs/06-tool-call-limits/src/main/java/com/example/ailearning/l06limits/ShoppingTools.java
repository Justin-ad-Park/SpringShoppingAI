package com.example.ailearning.l06limits;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ShoppingTools {
    public record Product(String productId, String name, int price) {}

    @Tool(description = "음료 상품을 검색합니다. 조회 후 같은 조건으로 반복 검색할 필요는 없습니다.")
    public List<Product> searchProducts(String category) {
        return "음료".equals(category)
                ? List.of(new Product("P01", "허브티", 12000)) : List.of();
    }

    @Tool(description = "검색한 상품 ID의 배송비를 원 단위로 조회합니다.")
    public int calculateShipping(String productId) {
        if (!"P01".equals(productId)) {
            throw new IllegalArgumentException("검색한 상품 ID를 사용하세요.");
        }
        return 3000;
    }
}
