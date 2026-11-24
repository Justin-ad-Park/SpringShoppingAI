package com.example.ailearning.l10rag.lesson03_indexing;

import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 하나의 정책 파일을 ## 항목별로 나누고 ID·원문·출처를 부여한다. 이 교재의 Markdown 형식 전용이다. */
public final class Lesson03PolicyLoader {
    private Lesson03PolicyLoader() {}
    public static List<Document> loadDocuments() throws IOException {
        String markdown;
        try (var input = new ClassPathResource("policies.md").getInputStream()) {
            markdown = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        var documents = new ArrayList<Document>();
        String[] sections = markdown.split("(?m)^## ");
        for (int sectionIndex = 1; sectionIndex < sections.length; sectionIndex++) {
            String[] titleAndBody = sections[sectionIndex]
                    .strip()
                    .split("\n", 2);
            if (titleAndBody.length != 2 || titleAndBody[1].isBlank()) throw new IllegalStateException("정책 절이 비었습니다.");
            String policyId = titleAndBody[0].strip();
            documents.add(Document.builder()
                            .id(policyId)
                            .text(titleAndBody[1].strip())
                            .metadata(Map.of("source", "policies.md#" + policyId))
                            .build());
        }
        return List.copyOf(documents);
    }
}
