package com.example.ailearning.l10rag.lesson03_indexing;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import static org.assertj.core.api.Assertions.assertThat;

/** JAR·하위 폴더 호환성을 확인하는 테스트. 기본 학습은 PolicyLoaderUnitTest를 먼저 읽는다. */
class Lesson03PolicyJarLoadingUnitTest {
    @TempDir Path temporaryDirectory;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void JAR의_하위_폴더까지_md만_읽고_상대_출처를_유지한다() throws Exception {
        Path policyJar = temporaryDirectory.resolve("policy-fixture.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(policyJar))) {
            addJarEntry(output, "policies/", "");
            addJarEntry(output, "policies/extra/", "");
            addJarEntry(output, "policies/policy.md", "# 배송\n\n## shipping\n기본 배송비는 3000원입니다.");
            addJarEntry(output, "policies/extra/policy.md", "# 반품\n\n## return-sealed\n미개봉 상품은 7일 이내 반품할 수 있습니다.");
            addJarEntry(output, "policies/ignored.txt", "Markdown이 아닌 자료는 읽지 않습니다.");
        }

        // 원래 앱의 정책 대신 임시 JAR만 검색하도록 클래스패스를 바꾸고, 끝나면 복원한다.
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try (var policyClassLoader = new URLClassLoader(new URL[]{policyJar.toUri().toURL()}, null)) {
            Thread.currentThread().setContextClassLoader(policyClassLoader);
            var documents = Lesson03PolicyLoader.loadDocuments();
            documents.forEach(document -> System.out.println("JAR 정책 출처: " + document.getMetadata().get("source")));
            assertThat(documents).extracting(document -> document.getId()).containsExactlyInAnyOrder("shipping", "return-sealed");
            assertThat(documents).extracting(document -> document.getMetadata().get("source")).containsExactlyInAnyOrder("policies/policy.md#shipping", "policies/extra/policy.md#return-sealed");
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private static void addJarEntry(JarOutputStream output, String path, String content) throws Exception {
        output.putNextEntry(new JarEntry(path));
        output.write(content.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }
}
