package com.example.ailearning.l08advisors.chain;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import java.util.List;

/** 08-4에서 전후 처리 순서를 기록하고, 08-5에서 전체 요청과 모델 요청의 범위를 비교한다. */
public record TraceAdvisor(String advisorName, int advisorOrder) implements CallAdvisor {
    public static final String TRACE_CONTEXT_KEY = "lesson.advisor.trace";

    @Override
    public String getName() {
        return advisorName;
    }

    @Override
    public int getOrder() {
        return advisorOrder;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // TraceAdvisor는 이름·순서만 보관하고, 실행 기록은 현재 요청의 context에서 받는다.
        // 이 키에는 서비스가 만든 List<String>을 전달한다. 해당 형 변환의 경고만 억제한다.
        @SuppressWarnings("unchecked")
        List<String> executionSteps = (List<String>) request.context().get(TRACE_CONTEXT_KEY);
        executionSteps.add(advisorName + ":before");
        try {
            // 다음 Advisor 또는 모델을 호출하고, 반환될 때까지 기다린다.
            return chain.nextCall(request);
        } finally {
            // 정상 응답과 실패 모두에서 종료를 기록한다. 성공했다는 표시가 아니다.
            executionSteps.add(advisorName + ":after");
        }
    }
}
