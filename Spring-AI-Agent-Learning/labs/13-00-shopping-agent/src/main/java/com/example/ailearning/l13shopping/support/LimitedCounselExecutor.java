package com.example.ailearning.l13shopping.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.*;
import java.util.function.Function;

/** 요청별 실행 상한과 취소를 담당한다. 작업은 대화 메모리를 수정하지 않는다. */
@Component
public class LimitedCounselExecutor {
    private static final Logger log = LoggerFactory.getLogger(LimitedCounselExecutor.class);
    public record Outcome<T>(T value, String failureStatus, RunState.Snapshot trace) {
        public boolean succeeded() { return failureStatus == null; }
    }
    private final int maxModels;
    private final int maxTools;
    private final long timeoutMs;

    public LimitedCounselExecutor(
            @Value("${learning.limits.max-model-calls}") int maxModels,
            @Value("${learning.limits.max-tool-calls}") int maxTools,
            @Value("${learning.limits.timeout-ms}") long timeoutMs) {
        this.maxModels = maxModels;
        this.maxTools = maxTools;
        this.timeoutMs = timeoutMs;
    }

    public <T> Outcome<T> execute(Function<RunState, T> work) {
        RunState run = new RunState(maxModels, maxTools, timeoutMs);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Future<T> task = executor.submit(() -> work.apply(run));
        try {
            T value = task.get(timeoutMs, TimeUnit.MILLISECONDS);
            run.check();
            return new Outcome<>(value, null, run.snapshot());
        } catch (TimeoutException error) {
            run.stop("TIMEOUT");
            task.cancel(true);
            return failed(run, error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            run.stop("CANCELLED");
            task.cancel(true);
            return failed(run, error);
        } catch (ExecutionException | RuntimeException error) {
            return failed(run, error instanceof ExecutionException ? error.getCause() : error);
        } finally {
            executor.shutdownNow();
        }
    }

    private <T> Outcome<T> failed(RunState run, Throwable error) {
        String status = run.stopped() == null ? "FAILED" : run.stopped();
        Throwable causeType = error;
        while (causeType.getCause() != null) causeType = causeType.getCause();
        // 원문 질문·응답·예외 메시지는 기록하지 않는다.
        log.warn("Counsel failed: stage={}, status={}, errorType={}",
                run.stage(), status, causeType.getClass().getSimpleName());
        return new Outcome<>(null, status, run.snapshot());
    }
}
