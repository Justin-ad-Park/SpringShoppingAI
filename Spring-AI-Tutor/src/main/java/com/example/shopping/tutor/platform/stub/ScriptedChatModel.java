package com.example.shopping.tutor.platform.stub;

import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.*;

import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;
import java.util.regex.*;

/** 준비된 실험만 재현하는 모델. 실제 언어 이해를 흉내 냈다고 주장하지 않는다. */
public class ScriptedChatModel implements ChatModel {
    @Override
    public ToolCallingChatOptions getOptions() {
        return ToolCallingChatOptions.builder().build();
    }

    public ChatResponse call(Prompt prompt) {
        String system = prompt.getSystemMessage().getText();
        String user = prompt.getUserMessage().getText();
        if (user.contains("[timeout]")) throw new IllegalStateException("학습용 timeout");
        if (prompt.getInstructions().getLast() instanceof ToolResponseMessage result) {
            return response("STUB 도구 결과: " + result.getResponses().stream()
                    .map(ToolResponseMessage.ToolResponse::responseData).collect(java.util.stream.Collectors.joining("\n")));
        }
        String answer;
        if (system.contains("TASK:CRITERIA")) {
            String category =
                    user.contains("음료")
                            ? "음료"
                            : user.contains("생활") ? "생활" : user.contains("식품") ? "식품" : null;
            Long budget = null;
            Matcher korean =
                    Pattern.compile("(-?\\d[\\d,]*)\\s*만(?:\\s*(\\d+)\\s*천)?\\s*원").matcher(user);
            Matcher plain = Pattern.compile("(-?\\d[\\d,]*)\\s*(천)?원").matcher(user);
            if (korean.find())
                budget =
                        Math.addExact(
                                Math.multiplyExact(
                                        Long.parseLong(korean.group(1).replace(",", "")), 10000),
                                korean.group(2) == null
                                        ? 0
                                        : Long.parseLong(korean.group(2)) * 1000);
            else if (plain.find())
                budget =
                        Math.multiplyExact(
                                Long.parseLong(plain.group(1).replace(",", "")),
                                plain.group(2) == null ? 1 : 1000);
            String keyword = user.contains("텀블러") ? "텀블러" : null;
            answer =
                    "{\"keyword\":"
                            + json(keyword)
                            + ",\"category\":"
                            + json(category)
                            + ",\"maxPrice\":"
                            + budget
                            + "}";
        } else if (system.contains("TASK:RECOMMENDATIONS"))
            answer =
                    "[{\"productId\":\"P004\",\"reason\":\"고정 후보 비교"
                            + " 실습\"},{\"productId\":\"P005\",\"reason\":\"고정 후보 비교 실습\"}]";
        else if (system.contains("TASK:JUDGE")
                || user.contains("Answer YES")
                || system.contains("Answer YES")) answer = user.contains("무관한답변") ? "NO" : "YES";
        else if (system.contains("TASK:REWRITE")) answer = "개봉한 상품 반품 조건";
        else if (system.contains("TASK:RAG") || user.contains("Context information"))
            answer = "학습용 정책 근거를 확인했습니다. 미개봉 상품은 수령 후 7일 이내 반품할 수 있습니다. 개봉 상품은 별도 확인이 필요합니다.";
        else if (prompt.getOptions() instanceof ToolCallingChatOptions options
                && options.getToolCallbacks() != null && !options.getToolCallbacks().isEmpty()) {
            // 모델은 도구 요청만 반환한다. 2.0 ToolCallingAdvisor가 실행하고 재호출한다.
            var defs = ToolCallingManager.builder().build().resolveToolDefinitions(options);
            String name =
                    defs.stream()
                            .map(d -> d.name())
                            .filter(
                                    n ->
                                            user.contains("배송") || user.contains("장바구니")
                                                    ? n.contains("Cart")
                                                    : n.contains("searchProducts"))
                            .findFirst()
                            .orElse(defs.getFirst().name());
            String args =
                    name.contains("searchProducts")
                            ? "{\"category\":\"음료\",\"maxPrice\":15000}"
                            : "{}";
            if (name.contains("searchProducts")
                    && defs.stream()
                            .filter(d -> d.name().equals(name))
                            .findFirst()
                            .orElseThrow()
                            .inputSchema()
                            .contains("\"query\"")) args = "{\"query\":" + args + "}";
            return
                    new ChatResponse(
                            List.of(
                                    new Generation(
                                            AssistantMessage.builder()
                                                    .content("")
                                                    .toolCalls(
                                                            List.of(
                                                                    new AssistantMessage.ToolCall(
                                                                            "stub-call",
                                                                            "function",
                                                                            name,
                                                                            args)))
                                                    .build())));
        } else
            answer =
                    "STUB 학습 응답: 예산·용도·취향을 먼저 확인하세요. 현재 재고는 도구 조회가 필요합니다. (전달 메시지 "
                            + prompt.getInstructions().size()
                            + "개)";
        return response(answer);
    }

    public Flux<ChatResponse> stream(Prompt prompt) {
        if (prompt.getUserMessage().getText().contains("[partial-error]"))
            return Flux.concat(
                    Flux.just(response("부분 "), response("응답")),
                    Flux.error(new IllegalStateException("학습용 스트림 중간 실패")));
        return Flux.defer(
                () -> {
                    String text = call(prompt).getResult().getOutput().getText();
                    List<ChatResponse> chunks = new ArrayList<>();
                    for (int i = 0; i < text.length(); i += 8)
                        chunks.add(response(text.substring(i, Math.min(i + 8, text.length()))));
                    return Flux.fromIterable(chunks).delayElements(Duration.ofMillis(40));
                });
    }

    public static ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private static String json(String text) {
        return text == null ? "null" : "\"" + text + "\"";
    }
}
