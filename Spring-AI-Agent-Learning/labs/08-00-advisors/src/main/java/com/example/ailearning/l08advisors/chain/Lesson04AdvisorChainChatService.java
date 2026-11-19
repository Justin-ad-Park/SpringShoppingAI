package com.example.ailearning.l08advisors.chain;

import com.example.ailearning.l08advisors.chain.support.AdvisorTrace;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/** 08-4: 기본 등록과 요청별 등록을 합친 뒤 order에 따라 전후 처리를 실행한다. */
@Service
public class Lesson04AdvisorChainChatService {
    public record Reply(String answer, List<String> steps) {}

    private final ChatClient chatClient;

    public Lesson04AdvisorChainChatService(ChatClient.Builder builder) {
        chatClient = builder
                // 이 ChatClient의 모든 요청에 적용한다.
                .defaultAdvisors(new TraceAdvisor("DefaultA", 0), new TraceAdvisor("DefaultB", 10))
                .build();
    }

    public Reply chat(String question) {
        var advisorTrace = new AdvisorTrace();
        String answer = chatClient
                .prompt()
                .user(question)
                // AdvisorA/B는 이번 요청에만 추가한다. 모든 요청에 적용하는 DefaultA/B도 함께 실행된다.
                .advisors(new TraceAdvisor("AdvisorA", 20), new TraceAdvisor("AdvisorB", 30))
                .advisors(advisorSpec -> advisorSpec.param(TraceAdvisor.TRACE_CONTEXT_KEY, advisorTrace))
                .call()
                .content();
        return new Reply(answer, advisorTrace.steps());
    }

    /** 다음 요청에 AdvisorA/B가 남지 않는지 비교하는 기본 등록 전용 호출. */
    public Reply chatWithDefaults(String question) {
        var advisorTrace = new AdvisorTrace();
        String answer = chatClient
                .prompt()
                .user(question)
                .advisors(advisorSpec -> advisorSpec.param(TraceAdvisor.TRACE_CONTEXT_KEY, advisorTrace))
                .call()
                .content();
        return new Reply(answer, advisorTrace.steps());
    }
}
