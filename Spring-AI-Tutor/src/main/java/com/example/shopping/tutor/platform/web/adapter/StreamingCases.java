package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l03streaming.StreamingLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 StreamingLesson부터 읽는다. */
@Component
public class StreamingCases {

    public Flux<String> stream(LessonExecution execution) {
        return new StreamingLesson().stream(execution.client(), execution.userInput());
    }

    @TutorCase(
            id = "L03-01",
            title = "스트리밍 응답",
            sample = "텀블러 고르는 기준 알려줘",
            goal = "전체 응답을 기다리지 않고 delta를 표시합니다.",
            stream = true,
            lesson = StreamingLesson.class,
            learningMethod = "stream")
    public Outcome streamBuyingGuide(LessonExecution execution) {
        return Outcome.text("이 케이스는 스트림 API로 실행하세요.");
    }

    @TutorCase(
            id = "L03-02",
            title = "생성 취소",
            sample = "선물 선택 기준을 설명해줘",
            goal = "정지 버튼으로 upstream 구독을 취소합니다.",
            stream = true,
            lesson = StreamingLesson.class,
            learningMethod = "stream")
    public Outcome cancelResponse(LessonExecution execution) {
        return Outcome.text("이 케이스는 스트림 API로 실행하세요.");
    }

    @TutorCase(
            id = "L03-03",
            title = "부분 응답과 실패",
            sample = "텀블러 선택 가이드 [partial-error]",
            goal = "부분 응답은 최종 성공으로 저장하지 않습니다. 실패 실험은 STUB 전용입니다.",
            stream = true,
            lesson = StreamingLesson.class,
            learningMethod = "stream")
    public Outcome handlePartialFailure(LessonExecution execution) {
        return Outcome.text("이 케이스는 스트림 API로 실행하세요.");
    }
}
