package com.example.shopping.tutor.lesson.l04memory.support;

import com.example.shopping.tutor.platform.stub.ScriptedChatModel;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/** AI 요청에 어떤 질문이 들어갔는지만 기록한다. 답변은 기존 연습용 모델이 만든다. */
public final class QuestionRecordingModel extends ScriptedChatModel {
    private List<String> questions = List.of();

    @Override
    public ChatResponse call(Prompt prompt) {
        questions =
                prompt.getInstructions().stream()
                        .filter(message -> message instanceof UserMessage)
                        .map(Message::getText)
                        .toList();
        return super.call(prompt);
    }

    public List<String> questionsInLastRequest() {
        return questions;
    }
}
