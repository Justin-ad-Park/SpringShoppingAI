package com.example.shopping.tutor.lesson.l04memory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;

/** DB에 저장한 뒤 새 저장 객체로 다시 읽는다. */
class JdbcMemoryLessonTest {
    @Test
    void 저장_객체를_새로_만들어도_DB에_남은_질문을_읽는다() {
        var restored = new JdbcMemoryLesson().persistAndRestore("예산은 3만원이야");
        assertThat(restored).extracting(Message::getText).containsExactly("예산은 3만원이야");
    }
}
