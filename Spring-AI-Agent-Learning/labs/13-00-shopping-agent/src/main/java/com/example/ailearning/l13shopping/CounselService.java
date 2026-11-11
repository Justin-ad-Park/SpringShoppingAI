package com.example.ailearning.l13shopping;

import com.example.ailearning.l13shopping.domain.Intent;
import com.example.ailearning.l13shopping.domain.ProductPriceDetails;
import com.example.ailearning.l13shopping.policy.PolicySearchResult;
import com.example.ailearning.l13shopping.support.LimitedAgentRequestExecutor;
import com.example.ailearning.l13shopping.support.RunState;
import com.example.ailearning.l13shopping.workflow.CounselResult;
import com.example.ailearning.l13shopping.workflow.CounselWorkflow;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
public class CounselService {
    public record Reply(String status, String answer, Intent criteria, List<ProductPriceDetails> offers,
            List<PolicySearchResult> sources, int storedMessages, RunState.Snapshot trace) {}

    private final CounselWorkflow counselWorkflow;
    private final LimitedAgentRequestExecutor limitedAgentRequestExecutor;
    private final ChatMemory conversationMemory = MessageWindowChatMemory.builder()
            .maxMessages(6)
            .build();

    public CounselService(CounselWorkflow counselWorkflow,
            LimitedAgentRequestExecutor limitedAgentRequestExecutor) {
        this.counselWorkflow = counselWorkflow;
        this.limitedAgentRequestExecutor = limitedAgentRequestExecutor;
    }

    public synchronized Reply chat(String owner, String conversationId, String question) {
        String conversationKey = owner + ":" + conversationId;
        List<Message> previousMessages = List.copyOf(conversationMemory.get(conversationKey));
        var executionOutcome = limitedAgentRequestExecutor.executeWithLimits(
                runState -> counselWorkflow.processCounselRequest(question, previousMessages, runState));
        if (!executionOutcome.succeeded()) {
            return createFailureReply(conversationKey, executionOutcome.trace(), executionOutcome.failureStatus());
        }

        CounselResult counselResult = executionOutcome.result();
        if (List.of("VERIFIED", "NEEDS_INPUT", "NO_RESULTS", "NO_CONTEXT").contains(counselResult.status())) {
            conversationMemory.add(conversationKey,
                    List.of(new UserMessage(question), new AssistantMessage(counselResult.answer())));
        }
        return new Reply(counselResult.status(), counselResult.answer(), counselResult.intent(),
                counselResult.offers(), counselResult.sources(), conversationMemory.get(conversationKey).size(),
                executionOutcome.trace());
    }

    private Reply createFailureReply(String conversationKey, RunState.Snapshot trace, String status) {
        return new Reply(status, "실행 실패 또는 상한 도달. 이전 대화는 유지합니다.", null,
                List.of(), List.of(), conversationMemory.get(conversationKey).size(), trace);
    }

    public synchronized List<String> history(String owner, String conversationId) {
        return conversationMemory.get(owner + ":" + conversationId)
                .stream()
                .map(Message::getText)
                .toList();
    }

    public synchronized void clear(String owner, String conversationId) {
        conversationMemory.clear(owner + ":" + conversationId);
    }
}
