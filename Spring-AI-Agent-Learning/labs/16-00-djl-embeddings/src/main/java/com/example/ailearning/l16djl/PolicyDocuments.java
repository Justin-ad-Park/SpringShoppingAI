package com.example.ailearning.l16djl;

import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;

/** 09단원과 같은 세 정책 문서. 다른 실습 모듈에는 의존하지 않는다. */
public final class PolicyDocuments {
    private PolicyDocuments() {}

    public static List<Document> createDocuments() {
        return List.of(
                createDocument("shipping", "shipping", "기본 배송비는 3000원이며 상품 금액 30000원 이상은 무료배송입니다."),
                createDocument("return-sealed", "returns", "미개봉 상품은 수령 후 7일 이내 반품할 수 있습니다."),
                createDocument("return-opened", "returns", "개봉한 상품은 단순 변심 반품이 불가합니다. 불량은 별도로 확인합니다."));
    }

    private static Document createDocument(String id, String category, String text) {
        return Document.builder()
                .id(id)
                .text(text)
                .metadata(Map.of("category", category))
                .build();
    }
}
