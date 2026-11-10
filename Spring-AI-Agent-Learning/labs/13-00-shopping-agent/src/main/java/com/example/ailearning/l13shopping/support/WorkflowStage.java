package com.example.ailearning.l13shopping.support;

/** 상담 Workflow가 현재 수행 중인 단계. */
public enum WorkflowStage {
    /** 사용자 요청에서 업무와 조건을 추출하는 단계. */
    INTENT,
    /** 필요한 상품·정책 도구를 호출하는 단계. */
    TOOLS,
    /** 도구 결과를 확인해 상담 응답을 만드는 단계. */
    VERIFY
}
