package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l11evaluation.EvaluationLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

import java.util.*;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 EvaluationLesson부터 읽는다. */
@Component
public class EvaluationCases {
    @TutorCase(
            id = "L11-01",
            title = "업무 사실 검증",
            sample = "P004와 P005의 가격과 배송비 확인",
            goal = "모델 판정 전에 결정적인 상품·금액 검증을 수행합니다.",
            lesson = EvaluationLesson.class,
            learningMethod = "checkBusinessFacts")
    public Outcome checkBusinessFacts(LessonExecution execution) {
        var result = new EvaluationLesson().checkBusinessFacts(execution.fixture);
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L11-02",
            title = "RelevancyEvaluator",
            sample = "미개봉 반품은 7일 이내인가요?",
            goal = "실제 Spring AI Evaluator를 사용하되 사실성 보증으로 오해하지 않습니다.",
            lesson = EvaluationLesson.class,
            learningMethod = "judgeRelevance")
    public Outcome judgeRelevance(LessonExecution execution) {
        var result =
                new EvaluationLesson().judgeRelevance(execution.model(), execution.userInput());
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L11-03",
            title = "회귀 질문셋",
            sample = "검색 조건 질문셋 실행",
            goal = "정답 라벨과 조건 추출 결과를 비교하고 실패를 보고합니다.",
            lesson = EvaluationLesson.class,
            learningMethod = "runRegressionDataset")
    public Outcome runRegressionDataset(LessonExecution execution) {
        var result = new EvaluationLesson().runRegressionDataset(execution.client());
        return new Outcome(result.status(), result.answer(), result.data());
    }
}
