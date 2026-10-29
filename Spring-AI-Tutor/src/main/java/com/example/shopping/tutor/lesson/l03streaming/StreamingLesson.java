package com.example.shopping.tutor.lesson.l03streaming;

import org.springframework.ai.chat.client.ChatClient;

import reactor.core.publisher.Flux;

/** 03: 응답 조각은 구독할 때 생성된다. 취소하면 provider까지 구독 취소를 전달한다. */
public final class StreamingLesson {
    /** .call().content()와 비교한다. 전체 문자열 대신 여러 조각을 Flux로 반환한다. */
    public Flux<String> stream(ChatClient chatClient, String question) {
        return chatClient.prompt().user(question).stream().content();
    }
}
