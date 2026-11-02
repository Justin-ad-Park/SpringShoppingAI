package com.example.shopping.tutor.platform.web;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 설명은 JSON, 짧은 코드는 실제 Java 파일에서 읽어 서로 달라지지 않게 한다. */
@Component
public final class BeginnerGuides {
    public record Guide(
            String id,
            String title,
            String why,
            String file,
            String method,
            String test,
            String testMethod,
            List<String> explanations,
            List<String> questions,
            String observe,
            String change,
            String testInstruction,
            String preparation,
            String modeNotice,
            String next) {}

    public record LessonGuide(Guide instructions, String code) {}

    private final Map<String, Guide> guides;

    public BeginnerGuides(ObjectMapper mapper) throws IOException {
        var resource = new ClassPathResource("learning/beginner-guides.json");
        try (var input = resource.getInputStream()) {
            List<Guide> entries = mapper.readValue(input, new TypeReference<>() {});
            guides =
                    entries.stream()
                            .collect(Collectors.toUnmodifiableMap(Guide::id, Function.identity()));
        }
    }

    public LessonGuide forCase(String caseId, List<LearningSources.LearningFile> files) {
        Guide guide = guides.get(caseId);
        if (guide == null) return null;
        try {
            String source = LearningSources.read(files, guide.file());
            String test = LearningSources.read(files, guide.test());
            if (!test.contains("void " + guide.testMethod() + "(")) {
                throw new IllegalStateException("따라하기 테스트를 찾을 수 없습니다: " + caseId);
            }
            return new LessonGuide(guide, methodCode(source, guide.method()));
        } catch (IOException error) {
            throw new IllegalStateException("따라하기 코드를 읽을 수 없습니다: " + caseId, error);
        }
    }

    // 현재 예제는 네 칸 들여쓰기의 public 메소드다. 복사한 별도 예제 코드는 두지 않는다.
    static String methodCode(String source, String method) {
        var declaration =
                Pattern.compile(
                                "(?m)^    public [^{;]+\\b"
                                        + Pattern.quote(method)
                                        + "\\([^)]*\\)\\s*\\{")
                        .matcher(source);
        if (!declaration.find()) throw new IllegalStateException("따라하기 메소드가 없습니다: " + method);
        int end = source.indexOf("\n    }", declaration.end());
        if (end < 0) throw new IllegalStateException("따라하기 메소드 끝이 없습니다: " + method);
        int start = declaration.start();
        var toolAnnotation =
                Pattern.compile("(?m)^    @Tool\\([^\\n]*\\)\\s*\\z")
                        .matcher(source.substring(0, start));
        if (toolAnnotation.find()) start = toolAnnotation.start();
        return source.substring(start, end + "\n    }".length()).stripIndent();
    }
}
