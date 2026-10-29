package com.example.shopping.tutor.platform.web;

import org.springframework.core.io.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.*;

/** 패키징한 수업 파일만 열 수 있다. 클라이언트가 보낸 로컬 경로는 직접 읽지 않는다. */
public final class LearningSources {
    private LearningSources() {}

    public record LearningFile(String role, String path, String label) {}

    public static List<LearningFile> forLesson(Class<?> lesson) {
        String packagePath = lesson.getPackageName().replace('.', '/');
        String primary = "src/main/java/" + lesson.getName().replace('.', '/') + ".java";
        List<LearningFile> files = new ArrayList<>();
        collect(files, "lesson-sources/", packagePath, "src/main/java/");
        collect(files, "lesson-test-sources/", packagePath, "src/test/java/");
        files.sort(
                Comparator.comparingInt((LearningFile file) -> rank(file, primary))
                        .thenComparing(LearningFile::path));
        return List.copyOf(files);
    }

    private static int rank(LearningFile file, String primary) {
        if (file.path().endsWith("README.md")) return 0;
        if (file.path().equals(primary)) return 1;
        return switch (file.role()) {
            case "핵심" -> 2;
            case "support" -> 3;
            default -> 4;
        };
    }

    private static void collect(
            List<LearningFile> files, String root, String packagePath, String sourceRoot) {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            for (Resource resource :
                    resolver.getResources("classpath*:" + root + packagePath + "/**/*")) {
                String url = resource.getURL().toString();
                String relative = url.substring(url.indexOf(root) + root.length());
                if (!relative.endsWith(".java") && !relative.endsWith("README.md")) continue;
                String role =
                        sourceRoot.contains("test")
                                ? "테스트"
                                : relative.endsWith("README.md")
                                        ? "학습 안내"
                                        : relative.contains("/support/") ? "support" : "핵심";
                String fileName = relative.substring(relative.lastIndexOf('/') + 1);
                files.add(new LearningFile(role, sourceRoot + relative, fileName));
            }
        } catch (IOException error) {
            throw new IllegalStateException("수업 소스 목록을 읽지 못했습니다.", error);
        }
    }

    public static String read(List<LearningFile> allowed, String requested) throws IOException {
        if (allowed.stream().noneMatch(file -> file.path().equals(requested)))
            throw new IllegalArgumentException("이 단원에 등록되지 않은 소스입니다.");
        String resource =
                requested.startsWith("src/test/java/")
                        ? "lesson-test-sources/" + requested.substring("src/test/java/".length())
                        : "lesson-sources/" + requested.substring("src/main/java/".length());
        return new ClassPathResource(resource)
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
