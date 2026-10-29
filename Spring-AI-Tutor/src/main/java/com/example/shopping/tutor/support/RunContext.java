package com.example.shopping.tutor.support;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.*;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.*;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import reactor.core.publisher.Flux;

/** 1회 실행의 관찰값과 staging memory. 성공한 실행만 웹 계층에서 commit한다. */
public class RunContext {
    public static final String SYSTEM="당신은 학습용 쇼핑 어시스턴트입니다. 한국어로 간결히 답하세요. 제공하지 않은 가격·재고·정책을 만들어내지 마세요.";
    public static class State {
        public List<Message> messages=new ArrayList<>();
        public Shop.Criteria criteria=new Shop.Criteria(null,null,null);
    }
    public final String message, fixture, experiment, conversationId;
    public final Models.Settings settings;
    public final EmbeddingModel embedding;
    public final ChatMemory memory=MessageWindowChatMemory.builder().maxMessages(10).build();
    public final Map<String,Object> diagnostics=new LinkedHashMap<>();
    public final List<Map<String,Object>> prompts=new ArrayList<>();
    public final List<Map<String,Object>> toolCalls=new ArrayList<>();
    public final List<String> advisorSteps=new ArrayList<>();
    public final Map<String,Object> options;
    public Shop.Criteria criteria;
    public org.springframework.core.io.Resource inputFile;
    public String inputMime;
    private final ChatModel observed;
    private final AtomicInteger calls=new AtomicInteger();
    private final long started=System.nanoTime();
    private final Consumer<Map<String,Object>> events;

    public RunContext(String message, String fixture, String experiment, String conversationId, Map<String,Object> options,
            Models.Settings settings, ChatModel model, EmbeddingModel embedding, State state, Consumer<Map<String,Object>> events) {
        if(message==null || message.isBlank() || message.length()>2000) throw new IllegalArgumentException("질문은 1~2000자여야 합니다.");
        Shop.subtotal(fixture);
        if(!Set.of("normal","invalid-json","partial-error","timeout","empty-retrieval").contains(experiment))
            throw new IllegalArgumentException("지원하지 않는 실패 실험입니다.");
        if(!settings.mode().equals("stub") && !experiment.equals("normal")) throw new IllegalArgumentException("실패 주입 실험은 stub 모드에서만 사용합니다.");
        this.message=message;this.fixture=fixture;this.experiment=experiment;this.conversationId=conversationId;
        this.options=options==null?Map.of():options;this.settings=settings;this.embedding=embedding;this.criteria=state.criteria;
        if(!this.options.keySet().stream().allMatch(Set.of("topK","temperature")::contains)) throw new IllegalArgumentException("허용되지 않은 옵션입니다.");
        topK();
        if(this.options.containsKey("temperature")) {
            Object value=this.options.get("temperature");
            if(!(value instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue()<0 || n.doubleValue()>1) throw new IllegalArgumentException("temperature는 0~1입니다.");
        }
        this.events=events==null?e -> {}:events;
        if(!state.messages.isEmpty()) memory.add(conversationId,state.messages);
        diagnostics.put("memoryBefore",messages(memory.get(conversationId)));
        observed=new ChatModel() {
            public ChatResponse call(Prompt prompt) {
                recordPrompt(prompt);
                ChatResponse response=model.call(prompt); recordUsage(response); return response;
            }
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.defer(() -> { recordPrompt(prompt); return model.stream(prompt).doOnNext(RunContext.this::recordUsage); });
            }
        };
    }
    public ChatModel model() { return observed; }
    public ChatClient client() { return ChatClient.builder(observed).defaultSystem(SYSTEM).build(); }
    public String userInput() { return message+(experiment.equals("timeout")?" [timeout]":experiment.equals("partial-error")?" [partial-error]":""); }
    public int topK() {
        Object n=options.getOrDefault("topK",3);
        if(!(n instanceof Number number) || !Double.isFinite(number.doubleValue()) || number.doubleValue()!=number.intValue() || number.intValue()<1 || number.intValue()>10) throw new IllegalArgumentException("topK는 1~10 정수입니다.");
        return number.intValue();
    }
    public void emit(String type, Map<String,Object> data) { events.accept(Map.of("type",type,"data",data)); }
    public void finish(State state) {
        state.messages=new ArrayList<>(memory.get(conversationId)); state.criteria=criteria;
        diagnostics.put("memoryAfter",messages(state.messages));
        diagnostics.put("prompts",List.copyOf(prompts));diagnostics.put("toolCalls",List.copyOf(toolCalls));
        diagnostics.put("advisorSteps",List.copyOf(advisorSteps));diagnostics.put("elapsedMs",(System.nanoTime()-started)/1_000_000);
        diagnostics.put("model",settings.mode().equals("stub")?"scripted (실제 언어 이해 아님)":settings.model());
    }
    public ToolCallback[] tools(Object bean) { return bounded(MethodToolCallbackProvider.builder().toolObjects(bean).build().getToolCallbacks()); }
    public ToolCallback[] bounded(ToolCallback[] callbacks) {
        return Arrays.stream(callbacks).map(t -> new ToolCallback() {
            public ToolDefinition getToolDefinition() { return t.getToolDefinition(); }
            public ToolMetadata getToolMetadata() { return t.getToolMetadata(); }
            public String call(String input) { return call(input,new ToolContext(Map.of())); }
            public String call(String input,ToolContext context) {
                if(calls.incrementAndGet()>8) throw new IllegalStateException("한 턴의 도구 실행 한도 8회를 초과했습니다.");
                long start=System.nanoTime();
                emit("tool-start",Map.of("name",t.getToolDefinition().name()));
                String result=t.call(input,context);
                var trace=Map.<String,Object>of("name",t.getToolDefinition().name(),"arguments",mask(input),"result",mask(result),"durationMs",(System.nanoTime()-start)/1_000_000);
                toolCalls.add(trace);emit("tool-result",trace);return result;
            }
        }).toArray(ToolCallback[]::new);
    }
    private void recordPrompt(Prompt p) {
        prompts.add(Map.of("messages",messages(p.getInstructions()),"options",p.getOptions()==null?"default":p.getOptions().getClass().getSimpleName()));
    }
    private void recordUsage(ChatResponse r) {
        if(r==null) throw new IllegalStateException("모델 응답이 없습니다.");
        var usage=r.getMetadata().getUsage();
        // 빈 usage는 provider가 실제 숫자를 제공한 것과 구분한다.
        if(!settings.mode().equals("stub") && usage!=null && usage.getTotalTokens()>0)
            diagnostics.put("usage",Map.of("inputTokens",usage.getPromptTokens(),"outputTokens",usage.getCompletionTokens(),"totalTokens",usage.getTotalTokens()));
    }
    public static List<Map<String,String>> messages(List<Message> list) {
        return list.stream().map(m -> Map.of("role",m.getMessageType().name(),"text",mask(m.getText()))).toList();
    }
    public static String mask(String text) {
        return text==null?"":text.replaceAll("01[016789][- ]?\\d{3,4}[- ]?\\d{4}","[전화번호 마스킹]")
            .replaceAll("(?i)sk-[a-zA-Z0-9_-]+","[키 마스킹]");
    }
}
