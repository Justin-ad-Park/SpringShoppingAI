package com.example.shopping.tutor.platform.execution;

import com.example.shopping.tutor.lesson.l06tools.support.DemoCartRepository;

import java.util.*;

/** 웹 입력 검증. 레슨은 검증된 질문과 옵션을 받는다. */
public final class LessonInput {
    private LessonInput() {}

    public static void validate(
            String message,
            String fixture,
            String experiment,
            Map<String, Object> options,
            String mode) {
        options = options == null ? Map.of() : options;
        if (message == null || message.isBlank() || message.length() > 2000)
            throw new IllegalArgumentException("질문은 1~2000자여야 합니다.");
        DemoCartRepository.subtotal(fixture);
        if (!Set.of("normal", "invalid-json", "partial-error", "timeout", "empty-retrieval")
                .contains(experiment)) throw new IllegalArgumentException("지원하지 않는 실패 실험입니다.");
        if (!mode.equals("stub") && !experiment.equals("normal"))
            throw new IllegalArgumentException("실패 주입 실험은 stub 모드에서만 사용합니다.");
        if (!options.keySet().stream().allMatch(Set.of("topK", "temperature")::contains))
            throw new IllegalArgumentException("허용되지 않은 옵션입니다.");
        topK(options);
        if (options.containsKey("temperature")) {
            Object value = options.get("temperature");
            if (!(value instanceof Number n)
                    || !Double.isFinite(n.doubleValue())
                    || n.doubleValue() < 0
                    || n.doubleValue() > 1)
                throw new IllegalArgumentException("temperature는 0~1입니다.");
        }
    }

    public static int topK(Map<String, Object> options) {
        Object n = options.getOrDefault("topK", 3);
        if (!(n instanceof Number number)
                || !Double.isFinite(number.doubleValue())
                || number.doubleValue() != number.intValue()
                || number.intValue() < 1
                || number.intValue() > 10) throw new IllegalArgumentException("topK는 1~10 정수입니다.");
        return number.intValue();
    }
}
