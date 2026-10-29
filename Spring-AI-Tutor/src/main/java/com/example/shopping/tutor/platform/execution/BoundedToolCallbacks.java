package com.example.shopping.tutor.platform.execution;

import static com.example.shopping.tutor.lesson.l05advisors.support.DiagnosticText.mask;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.*;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/** 같은 턴의 로컬·MCP 도구 모두에 실행 상한과 관찰을 적용한다. */
public final class BoundedToolCallbacks {
    private final AtomicInteger calls = new AtomicInteger();
    private final List<Map<String, Object>> traces;
    private final BiConsumer<String, Map<String, Object>> events;

    public BoundedToolCallbacks(
            List<Map<String, Object>> traces, BiConsumer<String, Map<String, Object>> events) {
        this.traces = traces;
        this.events = events;
    }

    public ToolCallback[] decorate(ToolCallback[] callbacks) {
        return Arrays.stream(callbacks)
                .map(
                        t ->
                                new ToolCallback() {
                                    public ToolDefinition getToolDefinition() {
                                        return t.getToolDefinition();
                                    }

                                    public ToolMetadata getToolMetadata() {
                                        return t.getToolMetadata();
                                    }

                                    public String call(String input) {
                                        return call(input, new ToolContext(Map.of()));
                                    }

                                    public String call(String input, ToolContext context) {
                                        if (calls.incrementAndGet() > 8)
                                            throw new IllegalStateException(
                                                    "한 턴의 도구 실행 한도 8회를 초과했습니다.");
                                        long start = System.nanoTime();
                                        events.accept(
                                                "tool-start",
                                                Map.of("name", t.getToolDefinition().name()));
                                        String result = t.call(input, context);
                                        var trace =
                                                Map.<String, Object>of(
                                                        "name",
                                                        t.getToolDefinition().name(),
                                                        "arguments",
                                                        mask(input),
                                                        "result",
                                                        mask(result),
                                                        "durationMs",
                                                        (System.nanoTime() - start) / 1_000_000);
                                        traces.add(trace);
                                        events.accept("tool-result", trace);
                                        return result;
                                    }
                                })
                .toArray(ToolCallback[]::new);
    }
}
