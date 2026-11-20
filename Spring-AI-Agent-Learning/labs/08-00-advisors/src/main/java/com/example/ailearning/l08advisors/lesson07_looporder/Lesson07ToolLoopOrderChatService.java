package com.example.ailearning.l08advisors.lesson07_looporder;

import com.example.ailearning.l08advisors.lesson05_order.OrderedLogAdvisor;
import com.example.ailearning.l08advisors.lesson06_toolloop.PriceTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.stereotype.Service;

/** 08-7: 08-5의 Advisor를 도구 반복 담당 객체의 앞과 뒤에 배치한다. */
@Service
public class Lesson07ToolLoopOrderChatService {
    private final ChatClient chatClient;
    private final PriceTools priceTools;

    public Lesson07ToolLoopOrderChatService(ChatClient.Builder builder, PriceTools priceTools) {
        chatClient = builder
                .defaultSystem("상품 가격은 반드시 제공된 도구로 확인한 값으로 안내하세요.")
                .build();
        this.priceTools = priceTools;
    }

    public String chat(String question) {
        int toolLoopOrder = ToolCallingAdvisor.DEFAULT_ORDER;
        return chatClient
                .prompt()
                .user(question)
                .advisors(new OrderedLogAdvisor("request", toolLoopOrder - 10),
                        new OrderedLogAdvisor("modelA", toolLoopOrder + 10),
                        new OrderedLogAdvisor("modelB", 1))
                .tools(priceTools)
                .call()
                .content();
    }
}
