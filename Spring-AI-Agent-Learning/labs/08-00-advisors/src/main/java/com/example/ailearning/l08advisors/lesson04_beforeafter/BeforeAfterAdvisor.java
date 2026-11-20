package com.example.ailearning.l08advisors.lesson04_beforeafter;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

/** 08-4: nextCall 앞과 정상 반환 뒤에 작성한 코드의 실행 위치를 확인한다. */
public class BeforeAfterAdvisor implements CallAdvisor {
    @Override
    public String getName() {
        return "single";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        System.out.println("single:before");
        ChatClientResponse response = chain.nextCall(request);
        System.out.println("single:after");
        return response;
    }
}
