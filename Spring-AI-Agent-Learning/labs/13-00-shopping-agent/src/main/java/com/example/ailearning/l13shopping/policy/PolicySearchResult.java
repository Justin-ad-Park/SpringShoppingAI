package com.example.ailearning.l13shopping.policy;
import org.springframework.ai.document.Document;

/** 정책 검색으로 찾은 문서 조각과 검색 정보를 담는다. */
public record PolicySearchResult(String id, String text, String location, Double score) {
    static PolicySearchResult fromDocument(Document document) {
        return new PolicySearchResult(document.getId(), document.getText(),
                (String) document.getMetadata().get("source"), document.getScore());
    }
}
