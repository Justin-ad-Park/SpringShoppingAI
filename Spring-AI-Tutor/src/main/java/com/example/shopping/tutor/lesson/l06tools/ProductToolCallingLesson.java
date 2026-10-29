package com.example.shopping.tutor.lesson.l06tools;

import com.example.shopping.tutor.lesson.l06tools.support.SandboxCart;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

import java.util.*;

/** 06 단원: 핵심 API를 직접 호출한다. 웹 실행·세션 확정은 platform에서 담당한다. */
public class ProductToolCallingLesson {

    private final SandboxCart sandbox;

    public ProductToolCallingLesson(SandboxCart sandbox) {
        this.sandbox = sandbox;
    }

    /** 핵심 동작을 분리해 단원 테스트에서 직접 검증한다. */
    public String callTool(ChatClient chatClient, String question, ToolCallback[] toolCallbacks) {
        return chatClient
                .prompt()
                .toolCallbacks(toolCallbacks)
                .toolContext(Map.of("customerId", "demo-trusted-customer"))
                .user(question)
                .call()
                .content();
    }

    /** 모델이 요청한 인자로 실제 @Tool 메소드를 실행합니다. */
    public Result searchProducts(
            ChatClient chatClient, String question, ToolCallback[] toolCallbacks) {
        return Result.text(callTool(chatClient, question, toolCallbacks));
    }

    /** 모델의 도구 선택과 Java 금액 계산을 나누어 관찰합니다. */
    public Result quoteFreeShipping(
            ChatClient chatClient, String question, String fixture, ToolCallback[] toolCallbacks) {
        return Result.ok(
                callTool(chatClient, question, toolCallbacks),
                Map.of(
                        "cart",
                        new CartSummaryTools(fixture)
                                .getCartSummary(
                                        new ToolContext(
                                                Map.of("customerId", "demo-trusted-customer")))));
    }

    /** 모델 인자 밖에서 서버가 고객 context를 전달합니다. */
    public Result passTrustedContext(
            ChatClient chatClient, String question, ToolCallback[] toolCallbacks) {
        return Result.text(callTool(chatClient, question, toolCallbacks));
    }

    /** 같은 실행 context에서 callback 8회 초과가 차단됩니다. */
    public Result boundToolExecution(ToolCallback[] toolCallbacks) {
        var tool =
                Arrays.stream(toolCallbacks)
                        .filter(t -> t.getToolDefinition().name().equals("searchProducts"))
                        .findFirst()
                        .orElseThrow();
        int allowed = 0;
        for (int i = 0; i < 9; i++) {
            try {
                tool.call("{\"category\":\"음료\",\"maxPrice\":15000}");
                allowed++;
            } catch (IllegalStateException expected) {
                return Result.ok("9번째 도구 호출을 차단했습니다.", Map.of("executed", allowed, "limit", 8));
            }
        }
        throw new IllegalStateException("실행 상한이 적용되지 않았습니다.");
    }

    /** 미리보기 후 명시적으로 확인해야 데모 장바구니가 변경됩니다. */
    public Result confirmSandboxCart(String question, String conversationId) {
        var match = java.util.regex.Pattern.compile("P\\d{3}").matcher(question);
        if (!match.find()) throw new IllegalArgumentException("상품 ID 예: P004를 입력하세요.");
        return Result.ok(
                "샌드박스 변경 제안입니다. 확인 버튼을 눌러야 반영됩니다.", sandbox.preview(conversationId, match.group()));
    }

    public record Result(String status, String answer, Map<String, Object> data) {
        public static Result ok(String answer, Map<String, Object> data) {
            return new Result("COMPLETED", answer, data);
        }

        public static Result text(String answer) {
            if (answer == null || answer.isBlank())
                throw new IllegalStateException("모델이 빈 응답을 반환했습니다.");
            return ok(answer, Map.of());
        }

        public static Result unsupported(String reason) {
            return new Result("UNSUPPORTED", reason, Map.of());
        }
    }
}
