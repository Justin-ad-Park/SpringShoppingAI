package com.example.ailearning.l13shopping.support;

import java.util.ArrayList;
import java.util.List;

/** 한 요청의 기록과 실행 상한. 커스텀 Advisor 없이 코드로 강제한다. */
public final class RunState {
    public record Step(String kind, String name, String detail) {}
    private final int maxModels, maxTools;
    private final long deadline;
    private int clients, models, tools;
    private String stopped;
    private final List<Step> steps = new ArrayList<>();

    public RunState(int maxModels, int maxTools, long timeoutMs) {
        this.maxModels = maxModels;
        this.maxTools = maxTools;
        this.deadline = System.nanoTime() + timeoutMs * 1_000_000L;
    }
    public synchronized void check() {
        if (stopped == null && System.nanoTime() >= deadline) stopped = "TIMEOUT";
        if (stopped != null) throw new IllegalStateException(stopped);
    }
    public synchronized void startClient() { check(); clients++; }
    public synchronized void startModel() {
        check();
        if (models >= maxModels) { stopped = "MODEL_LIMIT"; throw new IllegalStateException(stopped); }
        models++;
        steps.add(new Step("MODEL", "chat", "모델 요청 " + models));
    }
    public synchronized void startTool(String name, String input) {
        check();
        if (tools >= maxTools) { stopped = "TOOL_LIMIT"; throw new IllegalStateException(stopped); }
        tools++;
        steps.add(new Step("TOOL", name, input));
    }
    public synchronized void recordToolResult(String name, Object result) {
        check();
        steps.add(new Step("RESULT", name, String.valueOf(result)));
    }
    public synchronized String stopped() { return stopped; }
    public synchronized void stop(String reason) { if (stopped == null) stopped = reason; }
    public record Snapshot(int clientCalls, int modelCalls, int toolCalls, List<Step> steps) {}
    public synchronized Snapshot snapshot() { return new Snapshot(clients, models, tools, List.copyOf(steps)); }
}
