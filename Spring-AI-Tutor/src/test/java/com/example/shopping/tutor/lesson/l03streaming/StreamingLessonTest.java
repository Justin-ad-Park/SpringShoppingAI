package com.example.shopping.tutor.lesson.l03streaming;

import static com.example.shopping.tutor.platform.support.LessonExecutionFixture.context;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l04memory.*;
import com.example.shopping.tutor.lesson.l06tools.*;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.stub.ScriptedChatModel;
import com.example.shopping.tutor.platform.web.adapter.StreamingCases;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/** 단원 핵심 API를 직접 검증하고, 웹 연결 동작도 회귀 확인한다. */
class StreamingLessonTest {
    @Test
    void streamCancellationReachesUpstream() {
        var cancelled = new AtomicBoolean();
        ChatModel model =
                new ChatModel() {
                    public ChatResponse call(Prompt p) {
                        throw new UnsupportedOperationException();
                    }

                    public Flux<ChatResponse> stream(Prompt p) {
                        return Flux.<ChatResponse>never().doOnCancel(() -> cancelled.set(true));
                    }
                };
        var c = context("긴 답변", "normal", new LessonExecution.State(), model);
        StepVerifier.create(new StreamingCases().stream(c)).thenCancel().verify();
        assertThat(cancelled).isTrue();
    }

    @Test
    void partialStreamFailsWithoutCommittingHistory() {
        var c = context("[partial-error]");
        StepVerifier.create(new StreamingCases().stream(c))
                .expectNext("부분 ", "응답")
                .expectError()
                .verify();
        assertThat(c.memory.get(c.conversationId)).isEmpty();
    }

    @Test
    void streamEmitsFragmentsBeforeCompleting() {
        StepVerifier.create(
                        new StreamingLesson()
                                .stream(ChatClient.create(new ScriptedChatModel()), "선물 가이드"))
                .expectNextCount(1)
                .thenConsumeWhile(fragment -> !fragment.isEmpty())
                .verifyComplete();
    }
}
