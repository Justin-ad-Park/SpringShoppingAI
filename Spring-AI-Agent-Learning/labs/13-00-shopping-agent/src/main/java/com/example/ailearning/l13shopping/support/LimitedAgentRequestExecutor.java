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
    public record Outcome<T>(T result, String failureStatus, RunState.Snapshot trace) {
        public boolean succeeded() {
            return failureStatus == null;
        }
    }
    // 상담 한 건에서 허용할 ChatModel 요청 시도 횟수의 최댓값
    private final int maxModelRequestAttempts;
    // 상담 한 건의 전체 실행에 허용할 시간(밀리초)
    private final long timeoutMs;

    public LimitedAgentRequestExecutor(
            @Value("${learning.limits.max-model-calls}") int maxModelRequestAttempts,
            @Value("${learning.limits.timeout-ms}") long timeoutMs) {
        this.maxModelRequestAttempts = maxModelRequestAttempts;
        this.timeoutMs = timeoutMs;
    }

    public <T> Outcome<T> executeWithLimits(Function<RunState, T> requestWork) {
        RunState runState = new RunState(maxModelRequestAttempts, timeoutMs);
        ExecutorService taskExecutor = Executors.newVirtualThreadPerTaskExecutor();
        Future<T> requestTask = taskExecutor.submit(() -> requestWork.apply(runState));
        try {
            T requestResult = requestTask.get(timeoutMs, TimeUnit.MILLISECONDS);
            runState.check();
            return new Outcome<>(requestResult, null, runState.snapshot());
        } catch (TimeoutException timeoutException) {
            runState.recordStopReason(StopReason.TIMEOUT);
            requestTask.cancel(true);
            return failed(runState, timeoutException);
        } catch (InterruptedException interruptionException) {
            Thread.currentThread().interrupt();
            runState.recordStopReason(StopReason.CANCELLED);
            requestTask.cancel(true);
            return failed(runState, interruptionException);
        } catch (ExecutionException executionException) {
            // 작업이 예외로 끝난 뒤 전달된 예외이므로 완료된 requestTask를 다시 취소하지 않는다.
            runState.recordStopReason(StopReason.EXECUTION_FAILED);
            return failed(runState, executionException.getCause());
        } catch (RuntimeException runtimeException) {
            // 취소된 Future이거나 작업 완료 뒤 발생한 예외이므로 별도 취소 요청은 필요하지 않다.
            runState.recordStopReason(StopReason.EXECUTION_FAILED);
            return failed(runState, runtimeException);
        } finally {
            taskExecutor.shutdownNow();
        }
    }

    private <T> Outcome<T> failed(RunState runState, Throwable failureCause) {
        String failureStatus = runState.stopReason() == null ? "FAILED" : runState.stopReason().responseStatus();
        Throwable rootCause = failureCause;
        while (rootCause.getCause() != null) rootCause = rootCause.getCause();
        // 원문 질문·응답·예외 메시지는 기록하지 않는다.
        log.warn("Counsel failed: stage={}, status={}, errorType={}",
                runState.currentStage(), failureStatus, rootCause.getClass().getSimpleName());
        return new Outcome<>(null, failureStatus, runState.snapshot());
    }
}
