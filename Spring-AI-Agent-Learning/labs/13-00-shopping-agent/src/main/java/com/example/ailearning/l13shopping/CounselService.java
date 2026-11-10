package com.example.ailearning.l13shopping;

import com.example.ailearning.l13shopping.domain.Intent;
import com.example.ailearning.l13shopping.domain.ProductPriceDetails;

import com.example.ailearning.l13shopping.support.*;
import org.springframework.ai.chat.memory.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CounselService {
    public record Reply(String status, String answer, Intent criteria, List<ProductPriceDetails> offers,
                        List<Source> sources, int storedMessages, RunState.Snapshot trace) {}
    private final CounselWorkflow workflow;
    private final LimitedAgentRequestExecutor execution;
    private final ChatMemory memory = MessageWindowChatMemory.builder()
            .maxMessages(6)
            .build();
    public CounselService(CounselWorkflow workflow, LimitedAgentRequestExecutor execution) {
        this.workflow = workflow;
        this.execution = execution;
    }

    public synchronized Reply chat(String owner, String conversationId, String question) {
        String key = owner + ":" + conversationId;
        List<Message> previous = List.copyOf(memory.get(key));
        var outcome = execution.execute(run -> workflow.execute(question, previous, run));
        if (!outcome.succeeded()) {
            return failed(key, outcome.trace(), outcome.failureStatus());
        }
        CounselWorkflow.Draft draft = outcome.value();
        if (List.of("VERIFIED", "NEEDS_INPUT", "NO_RESULTS", "NO_CONTEXT").contains(draft.status())) {
            memory.add(key, List.of(new UserMessage(question), new AssistantMessage(draft.answer())));
        }
        return new Reply(draft.status(), draft.answer(), draft.intent(), draft.offers(), draft.sources(),
                memory.get(key).size(), outcome.trace());
    }

    private Reply failed(String key, RunState.Snapshot trace, String status) {
        return new Reply(status, "실행 실패 또는 상한 도달. 이전 대화는 유지합니다.", null,
                List.of(), List.of(), memory.get(key).size(), trace);
    }

    public synchronized List<String> history(String owner, String conversationId) {
        return memory
                .get(owner + ":" + conversationId)
                .stream()
                .map(Message::getText)
                .toList();
    }

    public synchronized void clear(String owner, String conversationId) {
        memory.clear(owner + ":" + conversationId);
    }
}
