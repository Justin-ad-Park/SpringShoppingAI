package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;
import com.example.shopping.tutor.lesson.l13shopping.ShoppingAssistantLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

import java.util.*;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 ShoppingAssistantLesson부터 읽는다. */
@Component
public class ShoppingAssistantCases {

    public SearchCriteria update(LessonExecution execution) {
        return new ShoppingAssistantLesson()
                .update(execution.client(), execution.userInput(), execution.criteria);
    }

    @TutorCase(
            id = "L13-01",
            title = "후속 조건 상품 추천",
            sample = "3만원 이하 선물 찾아줘",
            goal = "조건 추출 → Java 병합 → 상품 검색 → 사실 기반 설명을 조합합니다.",
            lesson = ShoppingAssistantLesson.class,
            learningMethod = "recommendWithFollowUp")
    public Outcome recommendWithFollowUp(LessonExecution execution) {
        var result =
                new ShoppingAssistantLesson()
                        .recommendWithFollowUp(
                                execution.client(), execution.userInput(), execution.criteria);
        if (result.data().get("criteria") instanceof SearchCriteria criteria)
            execution.criteria = criteria;
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L13-02",
            title = "무료배송 추천 통합",
            sample = "무료배송 채울 상품 추천해줘",
            goal = "현재 견적·부족액·사용자 예산·후보별 견적을 Java로 검증합니다.",
            lesson = ShoppingAssistantLesson.class,
            learningMethod = "recommendFreeShipping")
    public Outcome recommendFreeShipping(LessonExecution execution) {
        var result =
                new ShoppingAssistantLesson()
                        .recommendFreeShipping(
                                execution.client(),
                                execution.userInput(),
                                execution.fixture,
                                execution.criteria);
        if (result.data().get("criteria") instanceof SearchCriteria criteria)
            execution.criteria = criteria;
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L13-03",
            title = "상품 도구와 정책 RAG",
            sample = "텀블러 추천하고 반품 조건도 알려줘",
            goal = "상품 사실과 문서 근거를 각각 조회해 조합합니다.",
            lesson = ShoppingAssistantLesson.class,
            learningMethod = "combinePolicyAndCatalog")
    public Outcome combinePolicyAndCatalog(LessonExecution execution) {
        var result =
                new ShoppingAssistantLesson()
                        .combinePolicyAndCatalog(
                                execution.client(),
                                execution.userInput(),
                                execution.experiment,
                                execution.conversationId,
                                execution.embedding,
                                execution.memory,
                                execution.topK(),
                                execution::emit);
        if (result.data().get("criteria") instanceof SearchCriteria criteria)
            execution.criteria = criteria;
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L13-04",
            title = "실패 후 복구·초기화",
            sample = "3만원 이하 음료 찾아줘",
            goal = "실패 턴은 상태를 확정하지 않습니다. 실패 실험 후 이전 조건을 확인하세요.",
            lesson = ShoppingAssistantLesson.class,
            learningMethod = "recoverAndReset")
    public Outcome recoverAndReset(LessonExecution execution) {
        var result =
                new ShoppingAssistantLesson()
                        .recoverAndReset(
                                execution.client(),
                                execution.userInput(),
                                execution.experiment,
                                execution.criteria);
        if (result.data().get("criteria") instanceof SearchCriteria criteria)
            execution.criteria = criteria;
        return new Outcome(result.status(), result.answer(), result.data());
    }
}
