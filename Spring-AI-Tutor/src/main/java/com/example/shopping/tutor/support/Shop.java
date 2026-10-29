package com.example.shopping.tutor.support;

import java.util.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.chat.model.ToolContext;

/** 가격·배송비는 모델 응답 대신 Java에서 검증한다. 모든 데이터는 학습용이다. */
public final class Shop {
    private Shop() {}
    public record Product(String id, String name, long price, String category, String group, boolean inStock) {}
    public record Criteria(String keyword, String category, Long maxPrice) {
        public Criteria {
            if (maxPrice != null && maxPrice < 0) throw new IllegalArgumentException("예산은 0원 이상이어야 합니다.");
            if (category != null && !List.of("식품","음료","생활").contains(category))
                throw new IllegalArgumentException("지원하지 않는 카테고리입니다.");
        }
    }
    public record Recommendation(String productId, String reason) {}
    public static final List<Product> PRODUCTS = List.of(
        new Product("P001","데일리 그래놀라",18000,"식품","MALL",true),
        new Product("P002","오트 밀크 6팩",12000,"식품","MALL",true),
        new Product("P003","견과 스낵 세트",8900,"식품","MALL",true),
        new Product("P004","허브티 컬렉션",9900,"음료","MALL",true),
        new Product("P005","드립 커피 10개입",12500,"음료","MALL",true),
        new Product("P006","파트너 꿀 세트",8500,"식품","PARTNER",true),
        new Product("P007","프로틴 바 세트",9000,"식품","MALL",false),
        new Product("P008","미니 잼",4500,"식품","MALL",true),
        new Product("P009","텀블러",29000,"생활","MALL",true));
    public static Product product(String id) { return PRODUCTS.stream().filter(p -> p.id().equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다.")); }
    public static List<Product> search(Criteria c) {
        return PRODUCTS.stream().filter(Product::inStock)
            .filter(p -> c.category()==null || p.category().equals(c.category()))
            .filter(p -> c.maxPrice()==null || p.price()<=c.maxPrice())
            .filter(p -> c.keyword()==null || p.name().contains(c.keyword()))
            .sorted(Comparator.comparingLong(Product::price)).toList();
    }
    public static long subtotal(String fixture) { return switch(fixture) {
        case "standard" -> 42000; case "already-free" -> 60000; case "empty" -> 0; case "no-match" -> 1000;
        default -> throw new IllegalArgumentException("지원하지 않는 fixture입니다."); }; }
    public static long shipping(long subtotal) {
        if(subtotal<0) throw new IllegalArgumentException("음수 합계");
        return subtotal==0 || subtotal>=50000 ? 0 : 3000;
    }
    public static Map<String,Object> quote(String fixture, List<String> ids) {
        long subtotal = subtotal(fixture);
        for(String id: ids) {
            Product p=product(id);
            if(!p.inStock() || !p.group().equals("MALL")) throw new IllegalArgumentException("품절 또는 다른 배송그룹입니다.");
            subtotal=Math.addExact(subtotal,p.price());
        }
        return Map.of("projectedSubtotal",subtotal,"shippingFee",shipping(subtotal));
    }
    public static class Tools {
        private final String fixture;
        public Tools(String fixture) { this.fixture=fixture; subtotal(fixture); }
        @Tool(description="가격 상한(원)과 카테고리(식품/음료/생활)로 재고 있는 학습 상품을 검색합니다.")
        public List<Product> searchProducts(String category, long maxPrice) { return search(new Criteria(null,category,maxPrice)); }
        @Tool(description="현재 데모 장바구니의 합계, 무료배송 기준, 부족 금액을 조회합니다.")
        public Map<String,Object> getCartSummary(ToolContext context) {
            if(context.getContext().get("customerId")==null) throw new IllegalArgumentException("신뢰할 고객 context가 필요합니다.");
            long total=subtotal(fixture);
            return Map.of("subtotal", total, "threshold",50000,"missing",Math.max(0,50000-total),"shippingFee",shipping(total));
        }
    }
}
