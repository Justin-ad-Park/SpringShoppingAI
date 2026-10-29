package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l04memory.ConversationMemoryLesson;
import com.example.shopping.tutor.lesson.l04memory.JdbcMemoryLesson;
import com.example.shopping.tutor.lesson.l04memory.MessageWindowLesson;
import com.example.shopping.tutor.lesson.l04memory.TaskStateLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MemoryCases {
    public String chat(LessonExecution execution) {
        return new ConversationMemoryLesson(execution.model(), execution.memory)
                .chatWithMemory(execution.conversationId, execution.userInput());
    }

    @TutorCase(
            id = "L04-01",
            title = "대화 기억 비교",
            sample = "예산 3만원이야",
            goal = "다음 턴에서 이전 메시지가 모델에 전달되는지 관찰합니다.",
            lesson = ConversationMemoryLesson.class,
            learningMethod = "compareNoMemoryAndMemory")
    public Outcome compareNoMemoryAndMemory(LessonExecution execution) {
        var comparison =
                new ConversationMemoryLesson(execution.model(), execution.memory)
                        .compareNoMemoryAndMemory(execution.conversationId, execution.userInput());
        return Outcome.ok(
                comparison.withMemory(), Map.of("withoutMemory", comparison.withoutMemory()));
    }

    @TutorCase(
            id = "L04-02",
            title = "대화 격리와 초기화",
            sample = "나는 음료를 찾고 있어",
            goal = "새 대화·다른 탭의 메모리를 분리합니다.",
            lesson = ConversationMemoryLesson.class,
            learningMethod = "chatWithMemory")
    public Outcome isolateAndReset(LessonExecution execution) {
        return Outcome.text(chat(execution));
    }

    @TutorCase(
            id = "L04-03",
            title = "메시지 window",
            sample = "최근 메시지만 기억해줘",
            goal = "메시지 개수 제한과 토큰 제한의 차이를 확인합니다.",
            lesson = MessageWindowLesson.class,
            learningMethod = "keepRecentMessages")
    public Outcome windowMessages(LessonExecution execution) {
        var messages = new MessageWindowLesson().keepRecentMessages(4);
        return Outcome.ok(
                "최근 4개 메시지를 유지합니다. 토큰 예산 제한은 별도입니다.",
                Map.of("window", LessonExecution.messages(messages)));
    }

    @TutorCase(
            id = "L04-04",
            title = "JDBC memory 저장소",
            sample = "저장소를 다시 만들어도 대화가 남을까?",
            goal = "H2에 저장하고 새 JdbcChatMemoryRepository로 복구합니다.",
            lesson = JdbcMemoryLesson.class,
            learningMethod = "persistAndRestore")
    public Outcome persistWithJdbc(LessonExecution execution) {
        var restored = new JdbcMemoryLesson().persistAndRestore(execution.message);
        return Outcome.ok(
                "repository 인스턴스를 다시 만들어 저장된 메시지를 조회했습니다.",
                Map.of("restored", LessonExecution.messages(restored)));
    }

    @TutorCase(
            id = "L04-05",
            title = "대화 원문과 검색 상태",
            sample = "3만원 이하 음료 찾아줘",
            goal = "원문 기억과 검증된 업무 조건을 구분합니다. 다음 턴에 예산 해제를 입력해 보세요.",
            lesson = TaskStateLesson.class,
            learningMethod = "updateSearchCriteria")
    public Outcome compareTaskState(LessonExecution execution) {
        execution.criteria =
                new TaskStateLesson()
                        .updateSearchCriteria(
                                execution.client(), execution.criteria, execution.message);
        return Outcome.ok(chat(execution), Map.of("criteria", execution.criteria));
    }
}
