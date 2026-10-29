package com.example.shopping.tutor.lesson.l09rag.support;

import org.springframework.ai.document.Document;

import java.util.*;

/** 학습 결과 패널의 문서 표시 형식. 검색 동작에는 관여하지 않는다. */
public final class DocumentDisplay {
    private DocumentDisplay() {}

    public static List<Map<String, Object>> views(List<Document> docs) {
        return docs.stream()
                .map(
                        d -> {
                            Map<String, Object> v = new LinkedHashMap<>();
                            v.put("id", d.getId());
                            v.put("text", d.getText());
                            v.put("metadata", d.getMetadata());
                            v.put("score", d.getScore());
                            return v;
                        })
                .toList();
    }
}
