package com.example.ailearning.l13shopping.support;

/** 상담 요청을 중단한 원인과 API에 반환할 기존 상태 코드를 연결한다. */
public enum StopReason {
    /** 전체 요청에 허용한 시간이 지남. */
    TIMEOUT("TIMEOUT"),
    /** 요청별 모델 요청 시도 상한에 도달함. */
    MODEL_REQUEST_LIMIT("MODEL_LIMIT"),
    /** Spring AI가 요청별 전체 도구 실행 상한에 도달함. */
    TOOL_CALL_LIMIT("TOOL_LIMIT"),
    /** 호출 스레드가 중단되어 실행 취소를 요청함. */
    CANCELLED("CANCELLED");

    private final String responseStatus;

    StopReason(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String responseStatus() {
        return responseStatus;
    }
}
