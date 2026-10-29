package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l05advisors.AdvisorOrderLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 AdvisorOrderLesson부터 읽는다. */
@Component
public class AdvisorCases {
    @TutorCase(
            id = "L05-01",
            title = "Advisor 호출 순서",
            sample = "선물 고르는 기준 알려줘",
            goal = "A before → B before → 모델 → B after → A after를 확인합니다.",
            lesson = AdvisorOrderLesson.class,
            learningMethod = "traceOrder")
    public Outcome traceOrder(LessonExecution execution) {
        var result =
                new AdvisorOrderLesson()
                        .traceOrder(
                                execution.client(), execution.userInput(), execution.advisorSteps);
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L05-02",
            title = "진단 출력 마스킹",
            sample = "010-1234-5678로 배송 문의하고 싶어",
            goal = "모델 전달과 화면 진단 마스킹의 경계를 구분합니다.",
            lesson = AdvisorOrderLesson.class,
            learningMethod = "maskDiagnostics")
    public Outcome maskDiagnostics(LessonExecution execution) {
        var result =
                new AdvisorOrderLesson().maskDiagnostics(execution.client(), execution.userInput());
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L05-03",
            title = "Memory와 Trace 조합",
            sample = "내 예산은 3만원이야",
            goal = "advisor 조합 시 메모리 중복 저장과 실제 전달 이력을 확인합니다.",
            lesson = AdvisorOrderLesson.class,
            learningMethod = "composeMemoryAndTrace")
    public Outcome composeMemoryAndTrace(LessonExecution execution) {
        var result =
                new AdvisorOrderLesson()
                        .composeMemoryAndTrace(
                                execution.client(),
                                execution.userInput(),
                                execution.conversationId,
                                execution.memory,
                                execution.advisorSteps);
        return new Outcome(result.status(), result.answer(), result.data());
    }
}
