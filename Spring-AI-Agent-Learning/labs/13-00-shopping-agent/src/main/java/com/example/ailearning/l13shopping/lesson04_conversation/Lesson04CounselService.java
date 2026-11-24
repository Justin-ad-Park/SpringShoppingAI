package com.example.ailearning.l13shopping.lesson04_conversation;

import com.example.ailearning.l13shopping.domain.Intent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.ailearning.l13shopping.domain.ProductPriceDetail;
import com.example.ailearning.l13shopping.policy.PolicySearchResult;
import com.example.ailearning.l13shopping.workflow.CounselResult;
import com.example.ailearning.l13shopping.lesson02_intent.Lesson02CounselWorkflow;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class Lesson04CounselService {
    private static final Logger log = LoggerFactory.getLogger(Lesson04CounselService.class);
    public record Reply(String status, String answer, @JsonProperty("criteria") Intent intent, List<ProductPriceDetail> offers,
            List<PolicySearchResult> sources, int storedMessages) {}

    private final Lesson02CounselWorkflow counselWorkflow;
    private final ChatMemory conversationMemory = MessageWindowChatMemory.builder()
            .maxMessages(6)
            .build();

    public Lesson04CounselService(Lesson02CounselWorkflow counselWorkflow) {
        this.counselWorkflow = counselWorkflow;
    }

    public synchronized Reply chat(String owner, String conversationId, String question) {
        String conversationKey = owner + ":" + conversationId;
        List<Message> previousMessages = List.copyOf(conversationMemory.get(conversationKey));
        CounselResult counselResult;
        try {
            counselResult = counselWorkflow.processCounselRequest(question, previousMessages);
        } catch (RuntimeException error) {
            // 실패한 대화는 저장하지 않고 기존 대화는 유지한다.
            log.warn("Counsel failed: errorType={}", error.getClass().getSimpleName());
            return new Reply("FAILED", "상담 처리에 실패했습니다. 이전 대화는 유지합니다.", null,
                    List.of(), List.of(), conversationMemory.get(conversationKey).size());
        }
        if (List.of("VERIFIED", "NEEDS_INPUT", "NO_RESULTS", "NO_CONTEXT").contains(counselResult.status())) {
            conversationMemory.add(conversationKey,
                    List.of(new UserMessage(question), new AssistantMessage(counselResult.answer())));
        }
        return new Reply(counselResult.status(), counselResult.answer(), counselResult.intent(),
                counselResult.offers(), counselResult.sources(), conversationMemory.get(conversationKey).size());
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
