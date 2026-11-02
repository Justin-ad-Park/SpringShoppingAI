package com.example.shopping.agent;

import java.util.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.shopping.agent.adapters.McpShoppingClient;
import com.example.shopping.agent.capabilities.*;
import com.example.shopping.agent.conversation.*;
import com.example.shopping.agent.execution.McpSession;
import static com.example.shopping.agent.ChatDtos.*;
import static com.example.shopping.agent.conversation.ConversationModels.*;

/** Coordinates a turn. Business rules belong to capabilities; no task-specific tool interception. */
@Service
public class AgentService {
    private final ObjectMapper mapper;
    private final ToolCallbackProvider provider;
    private final IntentInterpreter interpreter;
    private final CriteriaReducer reducer;
    private final TaskStateStore memory;
    private final CapabilityRegistry registry;
    private final String mode;
    private final String apiKey;
    public AgentService(ObjectMapper mapper, ToolCallbackProvider provider, IntentInterpreter interpreter,
            CriteriaReducer reducer, TaskStateStore memory, CapabilityRegistry registry,
            @Value("${shopping.mode}") String mode, @Value("${spring.ai.openai.api-key}") String apiKey) {
        this.mapper = mapper; this.provider = provider; this.interpreter = interpreter; this.reducer = reducer;
        this.memory = memory; this.registry = registry; this.mode = mode; this.apiKey = apiKey;
    }
    public Response chat(Request request, String owner) {
        if (mode.equals("gpt") && (apiKey.isBlank() || apiKey.equals("unused-in-mock-mode")))
            throw new IllegalStateException("OPENAI_API_KEY 미설정");
        String conversation = request.conversationId() == null ? "default" : request.conversationId();
        return memory.turn(owner, conversation, request.expectedRevision(), snapshot -> {
            SearchTask active = snapshot.task();
            if (request.newSearch() || (active != null && !active.scenario().equals(request.scenario()))) active = null;
            Decision decision = interpreter.interpret(request.message(), active);
            long revision = snapshot.revision() + 1;
            if (decision.action() == Action.CLARIFY)
                return clarify(active, revision, decision.question());
            if (decision.action() == Action.REFINE && active == null)
                return clarify(null, revision, "이전 검색이 없거나 만료됐습니다. 검색 목적을 먼저 알려 주세요.");
            Capability capability;
            SearchTask next = active;
            try {
                String id = decision.action() == Action.REFINE ? active.capabilityId() : decision.capabilityId();
                capability = registry.require(id);
                if (capability.searchTask()) {
                    if (decision.action() != Action.NEW && decision.action() != Action.REFINE)
                        return clarify(active, revision, "새 검색인지 이전 조건 변경인지 알려 주세요.");
                    next = reducer.apply(decision, active, request.scenario());
                } else if (decision.action() != Action.INDEPENDENT) {
                    return clarify(active, revision, "조회 요청과 상품 검색 요청을 구분해 주세요.");
                }
            } catch (IllegalArgumentException e) {
                return clarify(active, revision, e.getMessage());
            }
            McpSession session = new McpSession(mapper, provider.getToolCallbacks());
            McpShoppingClient client = new McpShoppingClient(session, request.scenario());
            Capability.Result result = capability.execute(new Capability.Context(request.message(), next, decision, client, client, client));
            Response response = new Response(mode, result.message(), result.products(), result.details(), session.traces(),
                view(next), revision, decision.action().name(), result.outcome().name(), result.execution());
            // NO_RESULTS is valid user intent and is retained. Infrastructure failures throw before commit.
            return new TaskStateStore.Commit(next, response);
        });
    }
    private TaskStateStore.Commit clarify(SearchTask active, long revision, String question) {
        String message = question == null || question.isBlank() ? "검색 목적과 변경할 조건을 알려 주세요." : question;
        return new TaskStateStore.Commit(active, new Response(mode, message, List.of(), Map.of(), List.of(),
            view(active), revision, "CLARIFY", "NEEDS_INPUT", "NONE"));
    }
    private TaskView view(SearchTask task) { return task == null ? null : new TaskView(task.id(), task.capabilityId(), task.constraints()); }
    public String mode() { return mode; }
}
