package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l12operations.OperationsLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

import java.util.*;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 OperationsLesson부터 읽는다. */
@Component
public class OperationsCases {
    @TutorCase(
            id = "L12-01",
            title = "사용량과 지연 관측",
            sample = "텀블러 선택 기준 알려줘",
            goal = "provider usage와 실제 지연을 관찰합니다. 모델 호출은 Micrometer ObservationRegistry에 연결됩니다.",
            lesson = OperationsLesson.class,
            learningMethod = "recordUsageAndLatency")
    public Outcome recordUsageAndLatency(LessonExecution execution) {
        var result =
                new OperationsLesson()
                        .recordUsageAndLatency(execution.client(), execution.userInput());
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L12-02",
            title = "일시적 읽기 실패 재시도",
            sample = "503 다음 성공을 재현해줘",
            goal = "명시적으로 분류한 읽기 요청만 최대 2회 실행합니다. 변경 요청에는 적용하지 않습니다.",
            lesson = OperationsLesson.class,
            learningMethod = "retryTransientRead")
    public Outcome retryTransientRead(LessonExecution execution) {
        var result = new OperationsLesson().retryTransientRead();
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L12-03",
            title = "모델 계약 비교",
            sample = "선물 고르는 기준 알려줘",
            goal = "같은 ChatClient API에서 STUB과 설정된 모델 응답을 비교합니다.",
            lesson = OperationsLesson.class,
            learningMethod = "switchCompatibleModel")
    public Outcome switchCompatibleModel(LessonExecution execution) {
        var result =
                new OperationsLesson()
                        .switchCompatibleModel(
                                execution.client(),
                                execution.userInput(),
                                execution.settings.mode());
        return new Outcome(result.status(), result.answer(), result.data());
    }
}
