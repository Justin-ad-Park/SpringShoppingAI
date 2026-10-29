package com.example.shopping.tutor.lesson.l05advisors;

import com.example.shopping.tutor.lesson.l05advisors.support.DiagnosticText;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;

import java.util.List;
import java.util.Map;

/** 05 단원: 핵심 API를 직접 호출한다. 웹 실행·세션 확정은 platform에서 담당한다. */
public class AdvisorOrderLesson {

    /** A before → B before → 모델 → B after → A after를 확인합니다. */
    public Result traceOrder(ChatClient chatClient, String question, List<String> advisorSteps) {
        return Result.text(
                chatClient
                        .prompt()
                        .advisors(
                                new TraceAdvisor("A", 10, advisorSteps),
                                new TraceAdvisor("B", 20, advisorSteps))
                        .user(question)
                        .call()
                        .content());
    }

    /** 모델 전달과 화면 진단 마스킹의 경계를 구분합니다. */
    public Result maskDiagnostics(ChatClient chatClient, String question) {
        String answer = chatClient.prompt().user(question).call().content();
        return Result.ok(
                DiagnosticText.mask(answer),
                Map.of("notice", "관찰 패널은 마스킹합니다. 원문 provider 전송을 차단하는 기능과는 다릅니다."));
    }

    /** advisor 조합 시 메모리 중복 저장과 실제 전달 이력을 확인합니다. */
    public Result composeMemoryAndTrace(
            ChatClient chatClient,
            String question,
            String conversationId,
            ChatMemory chatMemory,
            List<String> advisorSteps) {
        return Result.text(
                chatClient
                        .prompt()
                        .advisors(
                                new TraceAdvisor("trace", 0, advisorSteps),
                                MessageChatMemoryAdvisor.builder(chatMemory).build())
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .user(question)
                        .call()
                        .content());
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
