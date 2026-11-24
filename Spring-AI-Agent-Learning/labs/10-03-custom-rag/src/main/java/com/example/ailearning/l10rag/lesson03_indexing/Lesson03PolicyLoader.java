package com.example.ailearning.l10rag.lesson03_indexing;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** policies/의 Markdown 파일을 읽고 ## 항목별 ID·원문·출처를 부여한다. */
public final class Lesson03PolicyLoader {
    private static final String POLICY_RESOURCE_PATTERN = "classpath*:policies/**/*.md";

    private Lesson03PolicyLoader() {}

    public static List<Document> loadDocuments() throws IOException {
        var documents = new ArrayList<Document>();
        for (Resource policyFile : findPolicyFiles()) {
            documents.addAll(loadPolicyFile(policyFile));
        }
        return List.copyOf(documents);
    }

    private static List<Resource> findPolicyFiles() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] policyFiles = resolver.getResources(POLICY_RESOURCE_PATTERN);
        if (policyFiles.length == 0) {
            throw new IOException("policies/ 폴더에서 Markdown 정책 파일을 찾을 수 없습니다.");
        }
        return Arrays.stream(policyFiles)
                .sorted(Comparator.comparing(Resource::getDescription))
                .toList();
    }

    private static List<Document> loadPolicyFile(Resource policyFile) throws IOException {
        String markdownContent = policyFile.getContentAsString(StandardCharsets.UTF_8);
        String sourcePath = getPolicySourcePath(policyFile);
        return extractPolicySections(markdownContent).stream()
                .map(section -> createPolicyDocument(section, sourcePath))
                .toList();
    }

    private static String getPolicySourcePath(Resource policyFile) throws IOException {
        // 일반 폴더와 JAR 내부 URL 모두 policies/부터의 경로만 출처에 남긴다.
        String resourceUrl = policyFile.getURL().toExternalForm();
        return resourceUrl.substring(resourceUrl.lastIndexOf("/policies/") + 1);
    }

    private static List<String> extractPolicySections(String markdownContent) {
        // 줄 시작의 "## "로 나눈 뒤, 첫 정책 항목 앞의 문서 소개는 제외한다.
        return Arrays.stream(markdownContent.split("(?m)^## "))
                .skip(1)
                .toList();
    }

    private static Document createPolicyDocument(String policySection, String sourcePath) {
        // 첫 줄은 ID, 나머지 전체는 본문으로 두어 조건과 예외를 함께 유지한다.
        String[] titleAndBody = policySection
                .strip()
                .split("\n", 2);
        if (titleAndBody.length != 2 || titleAndBody[1].isBlank()) {
            throw new IllegalStateException("정책 절이 비었습니다.");
        }
        String policyId = titleAndBody[0].strip();
        String policyText = titleAndBody[1].strip();
        return Document.builder()
                .id(policyId)
                .text(policyText)
                .metadata(Map.of("source", sourcePath + "#" + policyId))
                .build();
    }
}
