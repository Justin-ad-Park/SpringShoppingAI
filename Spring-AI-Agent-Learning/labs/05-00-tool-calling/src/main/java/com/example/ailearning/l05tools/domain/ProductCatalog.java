package com.example.ailearning.l05tools.domain;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;

@Service
public class ProductCatalog {
    private final List<Product> products = List.of(
            new Product("P01", "허브티", "음료", 12000),
            new Product("P02", "커피", "음료", 28000),
            new Product("P03", "텀블러", "생활", 35000));

    public List<Product> search(String category, Integer maxPrice) {
        if (category != null && !Set.of("음료", "생활")
                .contains(category)) {
            throw new IllegalArgumentException("category는 음료/생활 또는 null입니다.");
        }
        if (maxPrice != null && maxPrice < 0) throw new IllegalArgumentException("가격은 0 이상입니다.");
        return products
                .stream()
                .filter(product -> category == null || product
                        .category()
                        .equals(category))
                .filter(product -> maxPrice == null || product.price() <= maxPrice)
                .toList();
    }

    public ProductPriceDetail calculatePriceDetail(String productId) {
        Product product = products
                .stream()
                .filter(item -> item
                        .id()
                        .equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        int shippingFee = product.price() >= 30000 ? 0 : 3000;
        return new ProductPriceDetail(product, shippingFee, Math.addExact(product.price(), shippingFee));
    }
}
