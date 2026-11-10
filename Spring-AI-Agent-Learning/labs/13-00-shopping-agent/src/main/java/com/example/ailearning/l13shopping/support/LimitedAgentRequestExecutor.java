package com.example.ailearning.l13shopping.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.*;
import java.util.function.Function;

/** 에이전트 요청을 실행하고, 모델 요청 횟수와 전체 실행 시간을 제한하며 시간 초과 시 작업을 취소한다. */
@Component
public class LimitedAgentRequestExecutor {
    private static final Logger log = LoggerFactory.getLogger(LimitedAgentRequestExecutor.class);
    public record Outcome<T>(T value, String failureStatus, RunState.Snapshot trace) {
        public boolean succeeded() { return failureStatus == null; }
    }
    private final int maxModelRequestAttempts;
    private final long timeoutMs;

    public LimitedAgentRequestExecutor(
            @Value("${learning.limits.max-model-calls}") int maxModelRequestAttempts,
            @Value("${learning.limits.timeout-ms}") long timeoutMs) {
        this.maxModelRequestAttempts = maxModelRequestAttempts;
        this.timeoutMs = timeoutMs;
    }

    public <T> Outcome<T> execute(Function<RunState, T> work) {
        RunState run = new RunState(maxModelRequestAttempts, timeoutMs);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Future<T> task = executor.submit(() -> work.apply(run));
        try {
            T value = task.get(timeoutMs, TimeUnit.MILLISECONDS);
            run.check();
            return new Outcome<>(value, null, run.snapshot());
        } catch (TimeoutException error) {
            run.recordStopReason(StopReason.TIMEOUT);
            task.cancel(true);
            return failed(run, error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            run.recordStopReason(StopReason.CANCELLED);
            task.cancel(true);
            return failed(run, error);
        } catch (ExecutionException | RuntimeException error) {
            return failed(run, error instanceof ExecutionException ? error.getCause() : error);
        } finally {
            executor.shutdownNow();
        }
    }

    private <T> Outcome<T> failed(RunState run, Throwable error) {
        String status = run.stopReason() == null ? "FAILED" : run.stopReason().responseStatus();
        Throwable causeType = error;
        while (causeType.getCause() != null) causeType = causeType.getCause();
        // 원문 질문·응답·예외 메시지는 기록하지 않는다.
        log.warn("Counsel failed: stage={}, status={}, errorType={}",
                run.currentStage(), status, causeType.getClass().getSimpleName());
        return new Outcome<>(null, status, run.snapshot());
    }
}
