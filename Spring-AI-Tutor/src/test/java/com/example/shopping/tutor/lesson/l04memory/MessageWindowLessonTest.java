package com.example.shopping.tutor.lesson.l04memory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;

/** 저장할 메시지 수만 바꿔 비교한다. */
class MessageWindowLessonTest {
    @Test
    void 최근_네개_메시지만_남긴다() {
        var messages = new MessageWindowLesson().keepRecentMessages(4);
        assertThat(messages)
                .extracting(Message::getText)
                .containsExactly("질문 4", "답변 5", "질문 6", "답변 7");
    }

    @Test
    void 두개로_줄이면_마지막_질문과_답변만_남긴다() {
        var messages = new MessageWindowLesson().keepRecentMessages(2);
        assertThat(messages).extracting(Message::getText).containsExactly("질문 6", "답변 7");
    }
}
