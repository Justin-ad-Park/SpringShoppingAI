package com.example.ailearning.l10rag;

import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 읽기 → 제목 단위 분할 → 출처 메타데이터 부여. 짧은 학습 문서 전용이다. */
public final class PolicyLoader {
    private PolicyLoader() {}
    public static List<Document> load() throws IOException {
        String markdown;
        try (var input = new ClassPathResource("policies.md").getInputStream()) {
            markdown = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        var documents = new ArrayList<Document>();
        String[] sections = markdown.split("(?m)^## ");
        for (int i = 1; i < sections.length; i++) {
            String[] parts = sections[i].strip().split("\n", 2);
            if (parts.length != 2 || parts[1].isBlank()) throw new IllegalStateException("정책 절이 비었습니다.");
            String id = parts[0].strip();
            documents.add(Document.builder().id(id).text(parts[1].strip())
                    .metadata(Map.of("source", "policies.md#" + id)).build());
        }
        return List.copyOf(documents);
    }
}
