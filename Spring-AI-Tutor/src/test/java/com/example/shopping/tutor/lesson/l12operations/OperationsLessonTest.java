package com.example.shopping.tutor.lesson.l12operations;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l04memory.*;
import com.example.shopping.tutor.lesson.l06tools.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.ai.chat.memory.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/** 단원 핵심 API를 직접 검증하고, 웹 연결 동작도 회귀 확인한다. */
class OperationsLessonTest {
    @Test
    void transientReadHasBoundedRetriesAndUnauthorizedNeverRetries() {
        var calls = new AtomicInteger();
        assertThatThrownBy(
                        () ->
                                com.example.shopping.tutor.lesson.l12operations.OperationsLesson
                                        .retryRead(
                                                () -> {
                                                    calls.incrementAndGet();
                                                    throw new com.example.shopping.tutor.lesson
                                                            .l12operations.OperationsLesson
                                                            .ReadFailure(401);
                                                }))
                .isInstanceOf(
                        com.example.shopping.tutor.lesson.l12operations.OperationsLesson.ReadFailure
                                .class);
        assertThat(calls.get()).isEqualTo(1);
        calls.set(0);
        assertThatThrownBy(
                        () ->
                                com.example.shopping.tutor.lesson.l12operations.OperationsLesson
                                        .retryRead(
                                                () -> {
                                                    calls.incrementAndGet();
                                                    throw new com.example.shopping.tutor.lesson
                                                            .l12operations.OperationsLesson
                                                            .ReadFailure(503);
                                                }))
                .isInstanceOf(
                        com.example.shopping.tutor.lesson.l12operations.OperationsLesson.ReadFailure
                                .class);
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void transientExampleRecoversOnSecondAttempt() {
        assertThat(new OperationsLesson().retryTransientRead().data()).containsEntry("attempts", 2);
    }
}
