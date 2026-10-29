package com.example.shopping.tutor.platform.observation;

import static com.example.shopping.tutor.lesson.l05advisors.support.DiagnosticText.messages;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.*;

/** 한 실행의 화면 관찰값. 실제 provider 응답에 usage가 있을 때만 표시한다. */
public final class RunDiagnostics {
    public final Map<String, Object> diagnostics = new LinkedHashMap<>();
    public final List<Map<String, Object>> prompts = new ArrayList<>();
    public final List<Map<String, Object>> toolCalls = new ArrayList<>();
    public final List<String> advisorSteps = new ArrayList<>();
    private final boolean stub;

    public RunDiagnostics(boolean stub) {
        this.stub = stub;
    }

    public void recordPrompt(Prompt p) {
        prompts.add(
                Map.of(
                        "messages",
                        messages(p.getInstructions()),
                        "options",
                        p.getOptions() == null
                                ? "default"
                                : p.getOptions().getClass().getSimpleName()));
    }

    public void recordUsage(ChatResponse r) {
        if (r == null) throw new IllegalStateException("모델 응답이 없습니다.");
        var usage = r.getMetadata().getUsage();
        // 빈 usage는 provider가 실제 숫자를 제공한 것과 구분한다.
        if (!stub && usage != null && usage.getTotalTokens() > 0)
            diagnostics.put(
                    "usage",
                    Map.of(
                            "inputTokens",
                            usage.getPromptTokens(),
                            "outputTokens",
                            usage.getCompletionTokens(),
                            "totalTokens",
                            usage.getTotalTokens()));
    }
}
