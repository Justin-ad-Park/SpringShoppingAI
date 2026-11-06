package com.example.ailearning.l09search;

import org.springframework.ai.document.Document;
import java.util.List;
import java.util.Map;

/** 외부 파일/DB 없이 시작하는 가상 정책 문서. */
public final class PolicyDocuments {
    private PolicyDocuments() {}
    public static List<Document> all() {
        return List.of(
                document("shipping", "shipping", "기본 배송비는 3000원이며 상품 금액 30000원 이상은 무료배송입니다."),
                document("return-sealed", "returns", "미개봉 상품은 수령 후 7일 이내 반품할 수 있습니다."),
                document("return-opened", "returns", "개봉한 상품은 단순 변심 반품이 불가합니다. 불량은 별도로 확인합니다."));
    }
    private static Document document(String id, String category, String text) {
        return Document.builder()
                .id(id)
                .text(text)
                .metadata(Map.of("category", category))
                .build();
    }
}
