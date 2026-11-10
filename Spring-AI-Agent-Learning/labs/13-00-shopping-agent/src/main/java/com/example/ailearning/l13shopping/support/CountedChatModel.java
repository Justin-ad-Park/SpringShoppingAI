package com.example.ailearning.l13shopping.support;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

/** 실제 모델 요청 횟수를 기록하는 작은 위임 객체. 모델의 원래 옵션을 유지한다. */
public final class CountedChatModel implements ChatModel {
    private final ChatModel delegate;
    private final RunState run;

    public CountedChatModel(ChatModel delegate, RunState run) {
        this.delegate = delegate;
        this.run = run;
    }

    public ChatOptions getOptions() {
        return delegate.getOptions();
    }

    public ChatResponse call(Prompt prompt) {
        run.beginModelRequest();
        ChatResponse response = delegate.call(prompt);
        run.check();
        return response;
    }
}
