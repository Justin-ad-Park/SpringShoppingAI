package com.example.ailearning.l08advisors.chain.support;

import java.util.ArrayList;
import java.util.List;

/** 한 요청에서 실행한 Advisor의 전후 처리 기록. 요청마다 새 객체를 만든다. */
public class AdvisorTrace {
    private final List<String> steps = new ArrayList<>();

    public void record(String step) {
        steps.add(step);
    }

    public List<String> steps() {
        return List.copyOf(steps);
    }
}
