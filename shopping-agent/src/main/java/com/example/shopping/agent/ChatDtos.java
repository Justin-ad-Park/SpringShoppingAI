package com.example.shopping.agent;

import java.util.*;
import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import com.example.shopping.agent.conversation.ConversationModels.Criteria;

public final class ChatDtos {
    private ChatDtos() {}
    public record Request(@NotBlank @Size(max=2000) String message,
        @NotBlank @Pattern(regexp="standard|already-free|empty|no-match") String scenario,
        @Pattern(regexp="[a-zA-Z0-9_-]{1,64}") String conversationId,
        @PositiveOrZero Long expectedRevision, boolean newSearch) {}
    public record Trace(String tool, JsonNode arguments, JsonNode result, long durationMs) {}
    public record ShippingSummary(long projectedSubtotal, long shippingFee) {}
    public record Card(String id, String name, long price, String category, String emoji, ShippingSummary shipping) {}
    public record TaskView(String taskId, String capabilityId, Criteria constraints) {}
    public record Response(String mode, String message, List<Card> products, Map<String,Object> details,
        List<Trace> toolCalls, TaskView task, long revision, String action, String outcome, String execution) {}
}
