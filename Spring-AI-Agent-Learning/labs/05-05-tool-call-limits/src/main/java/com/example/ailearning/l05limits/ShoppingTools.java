package com.example.ailearning.l05limits;

import com.example.ailearning.l05limits.domain.Product;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ShoppingTools {
    @Tool(description = "음료, 생활, 간식 중 한 카테고리의 상품을 검색합니다. 한 번에 카테고리 하나만 전달하세요.")
    public List<Product> searchProducts(String category) {
        if (category == null) return List.of();
        return switch (category) {
            case "음료" -> List.of(new Product("P01", "허브티", 12000));
            case "생활" -> List.of(new Product("P03", "텀블러", 35000));
            case "간식" -> List.of(new Product("P04", "쿠키", 5000));
            default -> List.of();
        };
    }

    @Tool(description = "검색한 상품 ID의 배송비를 원 단위로 조회합니다.")
    public int calculateShipping(String productId) {
        if (productId == null || !List.of("P01", "P03", "P04").contains(productId)) {
            throw new IllegalArgumentException("검색한 상품 ID를 사용하세요.");
        }
        return "P03".equals(productId) ? 0 : 3000;
    }
}
