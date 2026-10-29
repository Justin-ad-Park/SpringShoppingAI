package com.example.shopping.tutor.lesson.l04memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shopping.tutor.lesson.l04memory.support.QuestionRecordingModel;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;

/** 같은 대화와 다른 대화를 비교한다. DB나 웹 서버는 사용하지 않는다. */
class ConversationMemoryLessonTest {
    @Test
    void 같은_대화에서는_이전_질문이_함께_전달된다() {
        // 준비: 대화 내용을 보관할 곳과 연습용 모델을 연결한다.
        var memory = MessageWindowChatMemory.builder().build();
        var model = new QuestionRecordingModel();
        var lesson = new ConversationMemoryLesson(model, memory);

        // 실행: 같은 대화 이름으로 질문을 두 번 보낸다.
        lesson.chatWithMemory("my-shopping", "예산은 3만원이야");
        lesson.chatWithMemory("my-shopping", "내 예산이 얼마였지?");

        // 확인: AI 요청에 앞 질문과 이번 질문이 함께 들어갔는지 본다.
        assertThat(model.questionsInLastRequest()).containsExactly("예산은 3만원이야", "내 예산이 얼마였지?");
        assertThat(memory.get("my-shopping"))
                .extracting(Message::getText)
                .contains("예산은 3만원이야", "내 예산이 얼마였지?");
    }

    @Test
    void 다른_대화에는_이전_질문을_전달하지_않는다() {
        var memory = MessageWindowChatMemory.builder().build();
        var model = new QuestionRecordingModel();
        var lesson = new ConversationMemoryLesson(model, memory);
        lesson.chatWithMemory("my-shopping", "예산은 3만원이야");

        lesson.chatWithMemory("other-shopping", "내 예산이 얼마였지?");

        assertThat(model.questionsInLastRequest()).containsExactly("내 예산이 얼마였지?");
        assertThat(memory.get("other-shopping"))
                .extracting(Message::getText)
                .doesNotContain("예산은 3만원이야");
    }
}
