package com.example.ailearning.l08advisors;

import org.springframework.ai.chat.client.*;
import org.springframework.ai.chat.client.advisor.api.*;
import java.util.List;

/** before는 다음 처리를 호출하기 전, after는 반환되거나 실패한 뒤에 기록한다. */
public record TraceAdvisor(String advisorName, int advisorOrder, List<String> advisorSteps) implements CallAdvisor {
    public String getName() { return advisorName; }
    public int getOrder() { return advisorOrder; }
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        advisorSteps.add(advisorName + ":before");
        try { return chain.nextCall(request); }
        finally { advisorSteps.add(advisorName + ":after"); }
    }
}
