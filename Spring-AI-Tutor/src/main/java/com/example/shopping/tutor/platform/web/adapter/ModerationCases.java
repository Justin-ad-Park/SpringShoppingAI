package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l14multimodal.ModerationLesson;
import com.example.shopping.tutor.platform.config.MediaModelConfiguration;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 ModerationLesson부터 읽는다. */
@Component
public class ModerationCases {
    private final MediaModelConfiguration models;

    public ModerationCases(MediaModelConfiguration models) {
        this.models = models;
    }

    @TutorCase(
            id = "L14-05",
            title = "Moderation 모델",
            sample = "상품 문의를 하고 싶어요",
            goal = "콘텐츠 판정 결과를 관찰합니다. 업무 권한 검증과는 별개입니다.",
            requires = "live",
            lesson = ModerationLesson.class,
            learningMethod = "checkAbusiveMessage")
    public Outcome checkAbusiveMessage(LessonExecution execution) {
        var result =
                new ModerationLesson()
                        .checkAbusiveMessage(
                                models.moderation,
                                execution.userInput(),
                                execution.settings.mode());
        return new Outcome(result.status(), result.answer(), result.data());
    }
}
