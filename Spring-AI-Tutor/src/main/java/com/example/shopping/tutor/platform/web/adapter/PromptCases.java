package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l01chat.PromptTemplateLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 PromptTemplateLesson부터 읽는다. */
@Component
public class PromptCases {
    @TutorCase(
            id = "L01-03",
            title = "템플릿 변수",
            sample = "예산 3만원, 동료 선물 추천 기준",
            goal = "변수 치환 후의 프롬프트를 확인합니다. 사용자 입력을 템플릿으로 실행하지 않습니다.",
            lesson = PromptTemplateLesson.class,
            learningMethod = "renderTemplate")
    public Outcome renderTemplate(LessonExecution execution) {
        var result =
                new PromptTemplateLesson()
                        .renderTemplate(execution.client(), execution.userInput());
        return Outcome.ok(
                result.answer(),
                java.util.Map.of("rendered", LessonExecution.mask(result.renderedPrompt())));
    }
}
