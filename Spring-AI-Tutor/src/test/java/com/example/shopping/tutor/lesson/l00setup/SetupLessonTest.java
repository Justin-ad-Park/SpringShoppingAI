package com.example.shopping.tutor.lesson.l00setup;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shopping.tutor.platform.stub.ScriptedChatModel;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

/** 처음 읽을 테스트. 서버나 API 키 없이 질문을 보내고 답변을 받는다. */
class SetupLessonTest {
    @Test
    void 질문을_보내면_답변_문자열을_받는다() {
        // 준비: 실제 API 대신 연습용 답변을 주는 모델을 연결한다.
        ChatClient chatClient = ChatClient.create(new ScriptedChatModel());
        SetupLesson lesson = new SetupLesson();

        // 실행: 화면에서 입력할 질문과 같은 문장을 전달한다.
        String answer = lesson.greet(chatClient, "음료를 추천해줘");

        System.out.println("Answer: " + answer);
        // 확인: 이 수업은 답변을 받는 방법을 배운다. 추천 상품의 정확성은 다음 수업에서 확인한다.
        assertThat(answer).isNotBlank();
    }
}
