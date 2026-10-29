package com.example.shopping.tutor.platform.observation;

import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;

/** 모델 요청·응답을 변경하지 않고 실제 요청과 사용량을 기록한다. */
public final class ObservedChatModel implements ChatModel {
    private final ChatModel delegate;
    private final RunDiagnostics observation;

    public ObservedChatModel(ChatModel delegate, RunDiagnostics observation) {
        this.delegate = delegate;
        this.observation = observation;
    }

    public ChatResponse call(Prompt prompt) {
        observation.recordPrompt(prompt);
        ChatResponse response = delegate.call(prompt);
        observation.recordUsage(response);
        return response;
    }

    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.defer(
                () -> {
                    observation.recordPrompt(prompt);
                    return delegate.stream(prompt).doOnNext(observation::recordUsage);
                });
    }
}
