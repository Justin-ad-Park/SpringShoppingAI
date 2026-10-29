package com.example.shopping.tutor.lesson.l12operations;

import com.example.shopping.tutor.platform.stub.ScriptedChatModel;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** 12 단원: 핵심 API를 직접 호출한다. 웹 실행·세션 확정은 platform에서 담당한다. */
public class OperationsLesson {

    public static class ReadFailure extends RuntimeException {
        public final int status;

        public ReadFailure(int status) {
            super("학습용 HTTP " + status);
            this.status = status;
        }
    }

    /** provider usage와 실제 지연을 관찰합니다. 모델 호출은 Micrometer ObservationRegistry에 연결됩니다. */
    public Result recordUsageAndLatency(ChatClient chatClient, String question) {
        return Result.text(chatClient.prompt().user(question).call().content());
    }

    /** 핵심 동작을 분리해 단원 테스트에서 직접 검증한다. */
    public static <T> T retryRead(Supplier<T> call) {
        for (int attempt = 1; ; attempt++) {
            try {
                return call.get();
            } catch (ReadFailure e) {
                if (attempt >= 2 || !Set.of(429, 503, 504).contains(e.status)) throw e;
            }
        }
    }

    /** 명시적으로 분류한 읽기 요청만 최대 2회 실행합니다. 변경 요청에는 적용하지 않습니다. */
    public Result retryTransientRead() {
        AtomicInteger calls = new AtomicInteger();
        String result =
                retryRead(
                        () -> {
                            if (calls.incrementAndGet() == 1) throw new ReadFailure(503);
                            return "읽기 요청 성공";
                        });
        return Result.ok(
                result,
                Map.of(
                        "attempts",
                        calls.get(),
                        "notice",
                        "결정적 장애 실험입니다. 일반 모델 호출의 숨은 자동 재시도는 비활성화했습니다."));
    }

    /** 같은 ChatClient API에서 STUB과 설정된 모델 응답을 비교합니다. */
    public Result switchCompatibleModel(ChatClient chatClient, String question, String mode) {
        ChatModel alternative = new ScriptedChatModel();
        String baseline = ChatClient.create(alternative).prompt().user(question).call().content();
        String configured = chatClient.prompt().user(question).call().content();
        return Result.ok(
                configured,
                Map.of(
                        "baseline",
                        baseline,
                        "configuredMode",
                        mode,
                        "notice",
                        "다른 OpenAI 호환 공급자는 서버의 TUTOR_OPENAI_BASE_URL과 모델 설정으로 실행 환경을 분리해 비교하세요."));
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
