package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;
import com.example.shopping.tutor.lesson.l02structured.StructuredOutputLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

import java.util.*;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 StructuredOutputLesson부터 읽는다. */
@Component
public class StructuredOutputCases {

    public SearchCriteria extract(LessonExecution execution) {
        return new StructuredOutputLesson().extract(execution.client(), execution.userInput());
    }

    @TutorCase(
            id = "L02-01",
            title = "자연어를 record로",
            sample = "3만원 이하 생활용품 찾아줘",
            goal = "entity() 변환과 음수 예산·카테고리 검증을 구분합니다.",
            lesson = StructuredOutputLesson.class,
            learningMethod = "extractCriteria")
    public Outcome extractCriteria(LessonExecution execution) {
        var result =
                new StructuredOutputLesson()
                        .extractCriteria(execution.client(), execution.userInput());
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L02-02",
            title = "제네릭 추천 목록",
            sample = "P004와 P005를 비교해줘",
            goal = "List 매핑 후 후보 ID와 중복을 검증합니다.",
            lesson = StructuredOutputLesson.class,
            learningMethod = "mapRecommendationList")
    public Outcome mapRecommendationList(LessonExecution execution) {
        var result =
                new StructuredOutputLesson()
                        .mapRecommendationList(execution.client(), execution.userInput());
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L02-03",
            title = "잘못된 JSON과 교정",
            sample = "3만원 이하 음료 찾아줘",
            goal = "변환 실패를 한 번만 교정하며 파싱과 업무 검증 오류를 구분합니다.",
            lesson = StructuredOutputLesson.class,
            learningMethod = "handleInvalidOutput")
    public Outcome handleInvalidOutput(LessonExecution execution) {
        var result =
                new StructuredOutputLesson()
                        .handleInvalidOutput(
                                execution.client(), execution.userInput(), execution.experiment);
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L02-04",
            title = "Native JSON Schema",
            sample = "3만원 이하 생활용품 찾아줘",
            goal = "일반 변환과 provider의 JSON Schema 옵션을 비교합니다.",
            requires = "native-schema",
            lesson = StructuredOutputLesson.class,
            learningMethod = "compareNativeSchema")
    public Outcome compareNativeSchema(LessonExecution execution) {
        var result =
                new StructuredOutputLesson()
                        .compareNativeSchema(
                                execution.client(),
                                execution.userInput(),
                                execution.settings.mode(),
                                execution.settings.nativeSchema());
        return new Outcome(result.status(), result.answer(), result.data());
    }
}
