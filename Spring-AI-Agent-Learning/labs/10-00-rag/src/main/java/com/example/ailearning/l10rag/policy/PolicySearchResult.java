package com.example.ailearning.l10rag.policy;

import org.springframework.ai.document.Document;

/** 정책 검색 결과의 ID·원문·원본 위치·유사도 점수. 답변에 인용됐다는 뜻은 아니다. */
public record PolicySearchResult(String id, String text, String location, Double score) {
    static PolicySearchResult fromDocument(Document document) {
        return new PolicySearchResult(document.getId(), document.getText(),
                (String) document.getMetadata().get("source"), document.getScore());
    }
}
