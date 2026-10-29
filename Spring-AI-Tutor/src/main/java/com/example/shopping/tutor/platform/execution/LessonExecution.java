package com.example.shopping.tutor.platform.execution;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;
import com.example.shopping.tutor.lesson.l05advisors.support.DiagnosticText;
import com.example.shopping.tutor.platform.config.AiModelConfiguration;
import com.example.shopping.tutor.platform.observation.ObservedChatModel;
import com.example.shopping.tutor.platform.observation.RunDiagnostics;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.*;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.*;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.util.*;
import java.util.function.Consumer;

/** 1회 실행의 관찰값과 staging memory. 성공한 실행만 웹 계층에서 commit한다. */
public class LessonExecution {
    public static final String SYSTEM =
            "당신은 학습용 쇼핑 어시스턴트입니다. 한국어로 간결히 답하세요. 제공하지 않은 가격·재고·정책을 만들어내지 마세요.";

    public static class State {
        public List<Message> messages = new ArrayList<>();
        public SearchCriteria criteria = new SearchCriteria(null, null, null);
    }

    public final String message, fixture, experiment, conversationId;
    public final AiModelConfiguration.Settings settings;
    public final EmbeddingModel embedding;
    public final ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(10).build();
    public final Map<String, Object> diagnostics;
    public final List<Map<String, Object>> prompts;
    public final List<Map<String, Object>> toolCalls;
    public final List<String> advisorSteps;
    public final Map<String, Object> options;
    public SearchCriteria criteria;
    public org.springframework.core.io.Resource inputFile;
    public String inputMime;
    private final ChatModel observed;
    private final RunDiagnostics observation;
    private final BoundedToolCallbacks boundedTools;
    private final long started = System.nanoTime();
    private final Consumer<Map<String, Object>> events;

    public LessonExecution(
            String message,
            String fixture,
            String experiment,
            String conversationId,
            Map<String, Object> options,
            AiModelConfiguration.Settings settings,
            ChatModel model,
            EmbeddingModel embedding,
            State state,
            Consumer<Map<String, Object>> events) {
        LessonInput.validate(message, fixture, experiment, options, settings.mode());
        this.message = message;
        this.fixture = fixture;
        this.experiment = experiment;
        this.conversationId = conversationId;
        this.options = options == null ? Map.of() : options;
        this.settings = settings;
        this.embedding = embedding;
        this.criteria = state.criteria;
        this.events = events == null ? e -> {} : events;
        observation = new RunDiagnostics(settings.mode().equals("stub"));
        diagnostics = observation.diagnostics;
        prompts = observation.prompts;
        toolCalls = observation.toolCalls;
        advisorSteps = observation.advisorSteps;
        boundedTools = new BoundedToolCallbacks(toolCalls, this::emit);
        if (!state.messages.isEmpty()) memory.add(conversationId, state.messages);
        diagnostics.put("memoryBefore", messages(memory.get(conversationId)));
        observed = new ObservedChatModel(model, observation);
    }

    public ChatModel model() {
        return observed;
    }

    public ChatClient client() {
        return ChatClient.builder(observed).defaultSystem(SYSTEM).build();
    }

    public String userInput() {
        return message
                + (experiment.equals("timeout")
                        ? " [timeout]"
                        : experiment.equals("partial-error") ? " [partial-error]" : "");
    }

    public int topK() {
        return LessonInput.topK(options);
    }

    public void emit(String type, Map<String, Object> data) {
        events.accept(Map.of("type", type, "data", data));
    }

    public void finish(State state) {
        state.messages = new ArrayList<>(memory.get(conversationId));
        state.criteria = criteria;
        diagnostics.put("memoryAfter", messages(state.messages));
        diagnostics.put("prompts", List.copyOf(prompts));
        diagnostics.put("toolCalls", List.copyOf(toolCalls));
        diagnostics.put("advisorSteps", List.copyOf(advisorSteps));
        diagnostics.put("elapsedMs", (System.nanoTime() - started) / 1_000_000);
        diagnostics.put(
                "model",
                settings.mode().equals("stub") ? "scripted (실제 언어 이해 아님)" : settings.model());
    }

    public ToolCallback[] bounded(ToolCallback[] callbacks) {
        return boundedTools.decorate(callbacks);
    }

    public ToolCallback[] tools(Object... bean) {
        return bounded(
                MethodToolCallbackProvider.builder().toolObjects(bean).build().getToolCallbacks());
    }

    public static List<Map<String, String>> messages(List<Message> messages) {
        return DiagnosticText.messages(messages);
    }

    public static String mask(String text) {
        return DiagnosticText.mask(text);
    }
}
