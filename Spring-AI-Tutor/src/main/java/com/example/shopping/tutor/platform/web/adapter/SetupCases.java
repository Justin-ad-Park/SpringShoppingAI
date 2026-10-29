package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l00setup.SetupLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 SetupLesson부터 읽는다. */
@Component
public class SetupCases {
    @TutorCase(
            id = "L00-01",
            title = "첫 ChatClient 호출",
            sample = "쇼핑 도우미야, 안녕",
            goal = "서버에서 모델을 호출하고 응답을 관찰합니다.",
            lesson = SetupLesson.class,
            learningMethod = "greet")
    public Outcome helloStub(LessonExecution execution) {
        var result = new SetupLesson().greet(execution.client(), execution.userInput());
        return Outcome.text(result);
    }

    @TutorCase(
            id = "L00-02",
            title = "실행 모드와 모델",
            sample = "현재 실행 환경을 알려줘",
            goal = "STUB과 LIVE, 자격증명의 서버 경계를 구분합니다.",
            lesson = SetupLesson.class,
            learningMethod = "inspectMode")
    public Outcome inspectMode(LessonExecution execution) {
        var result =
                new SetupLesson()
                        .inspectMode(execution.settings.mode(), execution.settings.model());
        return Outcome.ok(
                result.notice(), java.util.Map.of("mode", result.mode(), "model", result.model()));
    }
}
