package com.example.ailearning.l08advisors.toolloop;

import com.example.ailearning.l08advisors.chain.TraceAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 08-5: 메모리 없이 도구 루프 바깥과 안쪽의 실행 기록만 비교한다. */
@Service
public class Lesson05ToolLoopChatService {
    public record Reply(String answer, List<String> steps) {}

    private final ChatClient chatClient;
    private final PriceTools priceTools;

    public Lesson05ToolLoopChatService(ChatClient.Builder builder, PriceTools priceTools) {
        chatClient = builder
                .defaultSystem("상품 가격은 반드시 제공된 도구로 확인한 값으로 안내하세요.")
                .build();
        this.priceTools = priceTools;
    }

    public Reply chat(String question) {
        List<String> executionSteps = new ArrayList<>();
        int toolLoopOrder = ToolCallingAdvisor.DEFAULT_ORDER;
        String answer = chatClient
                .prompt()
                .user(question)
                // request는 전체 요청, model은 도구 결과로 반복되는 모델 요청을 감싼다.
                .advisors(new TraceAdvisor("request", toolLoopOrder - 10),
                        new TraceAdvisor("model", toolLoopOrder + 10))
                .advisors(advisorSpec -> advisorSpec.param(TraceAdvisor.TRACE_CONTEXT_KEY, executionSteps))
                .tools(priceTools)
                .call()
                .content();
        return new Reply(answer, List.copyOf(executionSteps));
    }
}
