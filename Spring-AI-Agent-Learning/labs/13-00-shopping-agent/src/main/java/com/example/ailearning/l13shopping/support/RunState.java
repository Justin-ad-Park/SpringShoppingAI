package com.example.ailearning.l13shopping.support;

import java.util.ArrayList;
import java.util.List;

/**
 * 상담 한 건 동안 모델 요청 수·전체 시간의 애플리케이션 상한을 확인하고 실행 내역을 기록한다.
 * 도구 실행 횟수는 Spring AI가 제한하며, 여기서는 실제 실행된 호출을 추적용으로 기록한다.
 * synchronized는 상담 작업과 시간 초과·취소 처리가 상태를 함께 다룰 때 기록을 보호한다.
 */
public final class RunState {
    /** 상담 흐름에서 발생한 단계와 관련 정보를 기록한다. */
    public record Step(StepKind kind, String name, String detail) {}

    // 이 상담에서 허용할 최대 ChatModel 요청 시도 횟수
    private final int maxModelRequestAttempts;
    // nanoTime 기준 마감 시각(나노초). 시스템 시각이 바뀌어도 남은 시간을 안정적으로 계산한다.
    private final long deadlineNanos;

    // ChatClient 생성 수, ChatModel 요청 시도 수, 실제 도구 실행 수를 각각 기록한다.
    private int clientCreations, modelRequestAttempts, toolExecutions;
    // 상담 실행을 중단한 원인. 처음 기록된 원인을 유지한다.
    private StopReason stopReason;
    // 실패 로그에 남길 Workflow의 현재 단계.
    private WorkflowStage currentStage = WorkflowStage.INTENT;
    // 요청 순서대로 쌓아 응답에 실행 내역으로 전달한다.
    private final List<Step> steps = new ArrayList<>();

    public RunState(int maxModelRequestAttempts, long timeoutMs) {
        this.maxModelRequestAttempts = maxModelRequestAttempts;
        // nanoTime은 경과 시간 측정용이므로 timeout을 같은 단위인 나노초로 바꾼다.
        this.deadlineNanos = System.nanoTime() + timeoutMs * 1_000_000L;
    }

    /** 호출 전에 마감 시각을 확인하고, 이미 중단된 요청이면 그 이유를 예외로 알린다. */
    public synchronized void check() {
        if (stopReason == null && System.nanoTime() >= deadlineNanos) {
            stopReason = StopReason.TIMEOUT;
        }
        if (stopReason != null) throw new IllegalStateException(stopReason.responseStatus());
    }

    /** ChatClient 인스턴스를 만든 횟수를 기록한다. */
    public synchronized void recordClientCreation() { check(); clientCreations++; }

    /** ChatModel.call을 시작하기 직전에 상한을 확인하고 요청 시도를 기록한다. */
    public synchronized void beginModelRequest() {
        check();
        if (modelRequestAttempts >= maxModelRequestAttempts) {
            stopReason = StopReason.MODEL_REQUEST_LIMIT;
            throw new IllegalStateException(stopReason.responseStatus());
        }
        modelRequestAttempts++;
        steps.add(new Step(StepKind.MODEL, "chat", "모델 요청 " + modelRequestAttempts));
    }

    /** Spring AI가 허용한 도구 실행을 추적 횟수와 단계 기록에 추가한다. */
    public synchronized void beginToolExecution(String name, String input) {
        check();
        toolExecutions++;
        steps.add(new Step(StepKind.TOOL, name, input));
    }

    /** 도구 실행이 끝난 뒤 반환 결과를 실행 내역에 남긴다. */
    public synchronized void recordToolResult(String name, Object result) {
        check();
        steps.add(new Step(StepKind.RESULT, name, String.valueOf(result)));
    }

    /** Workflow가 다음 작업 단계에 진입할 때 현재 단계를 기록한다. */
    public synchronized void enterStage(WorkflowStage stage) {
        check();
        this.currentStage = stage;
    }

    public synchronized WorkflowStage currentStage() { return currentStage; }
    public synchronized StopReason stopReason() { return stopReason; }

    /** 실행기나 Workflow가 중단 원인을 기록한다. 최초 원인은 후속 원인으로 덮어쓰지 않는다. */
    public synchronized void recordStopReason(StopReason reason) {
        if (stopReason == null) stopReason = reason;
    }

    /** 상담 응답에 담아 반환할 객체 생성 수·요청 시도 수·도구 실행 수와 실행 내역이다. */
    public record Snapshot(int clientCreations, int modelRequestAttempts, int toolExecutions, List<Step> steps) {}
    public synchronized Snapshot snapshot() {
        return new Snapshot(clientCreations, modelRequestAttempts, toolExecutions, List.copyOf(steps));
    }
}
