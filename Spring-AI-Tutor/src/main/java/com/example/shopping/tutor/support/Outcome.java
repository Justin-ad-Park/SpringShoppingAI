package com.example.shopping.tutor.support;

import java.util.*;

public record Outcome(String status, String answer, Map<String, Object> data) {
    public static Outcome ok(String answer, Map<String,Object> data) { return new Outcome("COMPLETED", answer, data); }
    public static Outcome text(String answer) {
        if (answer == null || answer.isBlank()) throw new IllegalStateException("모델이 빈 응답을 반환했습니다.");
        return ok(answer, Map.of());
    }
    public static Outcome unsupported(String reason) { return new Outcome("UNSUPPORTED", reason, Map.of()); }
}
