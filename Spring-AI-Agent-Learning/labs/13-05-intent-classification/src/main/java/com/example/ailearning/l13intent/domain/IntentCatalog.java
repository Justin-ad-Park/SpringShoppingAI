package com.example.ailearning.l13intent.domain;

import java.util.List;

/** 업무에서 관리하는 분류표다. 모델이 새 분류를 추가하지 않는다. */
public record IntentCatalog(String version, List<Major> majors) {
    public record Option(String code, String description) {}
    public record Major(String code, String description, List<Option> children) {}

    public List<Option> majorOptions() {
        return majors.stream()
                .map(major -> new Option(major.code(), major.description()))
                .toList();
    }

    public List<Option> minorOptions(String majorCode) {
        return majors.stream()
                .filter(major -> major.code().equals(majorCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 대분류입니다."))
                .children();
    }
}
