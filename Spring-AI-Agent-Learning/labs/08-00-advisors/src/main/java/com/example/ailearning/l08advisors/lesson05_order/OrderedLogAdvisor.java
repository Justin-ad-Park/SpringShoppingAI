package com.example.ailearning.l08advisors.lesson05_order;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

/** 08-5: 이름과 순서만 바꾸어 08-4와 같은 전후 처리를 비교한다. */
public record OrderedLogAdvisor(String advisorName, int advisorOrder) implements CallAdvisor {
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
        System.out.println(advisorName + ":before");
        ChatClientResponse response = chain.nextCall(request);
        System.out.println(advisorName + ":after");
        return response;
    }
}
