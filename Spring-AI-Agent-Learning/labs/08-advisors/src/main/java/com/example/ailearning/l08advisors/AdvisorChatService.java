package com.example.ailearning.l08advisors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.memory.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdvisorChatService {
    public record Reply(String answer, List<String> steps, int storedMessages) {}
    private final ChatClient client;
    private final ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(6).build();

    public AdvisorChatService(ChatClient.Builder builder) {
        // 07에서 직접 작성했던 메시지 조회·추가·저장을 메모리 Advisor가 담당한다.
        client = builder.defaultSystem("전달된 대화를 참고해 답하세요. 가격은 제공된 도구 결과만 사용하세요.")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build()).build();
    }

    public synchronized Reply chat(String owner, String conversationId, String question, boolean useTool) {
        var steps = new ArrayList<String>();
        int loopOrder = ToolCallingAdvisor.DEFAULT_ORDER;
        var request = client.prompt().user(question)
                .advisors(new TraceAdvisor("A", loopOrder - 10, steps),
                        new TraceAdvisor("B", loopOrder + 10, steps))
                .advisors(parameters -> parameters.param(ChatMemory.CONVERSATION_ID, owner + ":" + conversationId));
        if (useTool) request.tools(new PriceTools());
        String answer = request.call().content();
        // memory.add()를 다시 호출하면 중복 저장될 수 있으므로 추가하지 않는다.
        return new Reply(answer, List.copyOf(steps), memory.get(owner + ":" + conversationId).size());
    }

    public synchronized void clear(String owner, String conversationId) { memory.clear(owner + ":" + conversationId); }
}
