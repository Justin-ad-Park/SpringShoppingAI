package com.example.shopping.tutor.lesson.l14multimodal;

import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.ai.openai.*;

import java.util.Map;

/** 14 단원: 핵심 API를 직접 호출한다. 웹 실행·세션 확정은 platform에서 담당한다. */
public class ModerationLesson {

    /** 콘텐츠 판정 결과를 관찰합니다. 업무 권한 검증과는 별개입니다. */
    public Result checkAbusiveMessage(
            OpenAiModerationModel moderationModel, String question, String mode) {
        if (mode.equals("stub")) return Result.unsupported("실제 moderation 판정은 LIVE에서 실행하세요.");
        var result = moderationModel.call(new ModerationPrompt(question)).getResult().getOutput();
        return Result.ok("콘텐츠 모델의 판정입니다. 장애 시 안전하다고 확정하지 않습니다.", Map.of("moderation", result));
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
