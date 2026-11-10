package com.example.ailearning.l13shopping.support;

/** 상담 trace에 기록하는 단계의 종류. */
public enum StepKind {
    /** 모델에 요청을 보낸 시도. */
    MODEL,
    /** Spring AI가 허용해 실제 시작된 도구 실행. */
    TOOL,
    /** 도구 실행이 반환한 결과. */
    RESULT
}
