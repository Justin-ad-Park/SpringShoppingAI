package com.example.ailearning.l07memory;

import org.springframework.ai.chat.messages.Message;

/** 학습 화면/HTTP 응답에서 역할과 내용을 읽기 위한 자료형. */
public record MessageView(String role, String content) {
    static MessageView from(Message message) {
        return new MessageView(message.getMessageType().getValue(), message.getText());
    }
}
