package com.example.shopping.tutor.lesson.l05advisors;

import org.springframework.ai.chat.client.*;
import org.springframework.ai.chat.client.advisor.api.*;

import java.util.List;

/** 순서 숫자를 바꾸고 실제 호출 전후 기록을 비교하는 교육용 Advisor. */
public record TraceAdvisor(String name, int order, List<String> steps) implements CallAdvisor {
    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        steps.add(name + ":before");
        try {
            return chain.nextCall(request);
        } finally {
            steps.add(name + ":after");
        }
    }
}
