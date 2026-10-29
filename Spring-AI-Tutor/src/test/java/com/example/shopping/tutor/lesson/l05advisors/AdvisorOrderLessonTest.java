package com.example.shopping.tutor.lesson.l05advisors;

import static com.example.shopping.tutor.platform.support.LessonExecutionFixture.context;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l04memory.*;
import com.example.shopping.tutor.lesson.l06tools.*;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.stub.ScriptedChatModel;
import com.example.shopping.tutor.platform.web.adapter.AdvisorCases;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/** 단원 핵심 API를 직접 검증하고, 웹 연결 동작도 회귀 확인한다. */
class AdvisorOrderLessonTest {
    @Test
    void memoryAndTraceDoNotSaveTwice() {
        var c = context("내 예산 3만원");
        new AdvisorCases().composeMemoryAndTrace(c);
        assertThat(c.memory.get(c.conversationId)).hasSize(2);
        assertThat(c.advisorSteps).containsExactly("trace:before", "trace:after");
    }

    @Test
    void diagnosticsMaskPhoneAndKeys() {
        assertThat(LessonExecution.mask("010-1234-5678 sk-example-key"))
                .doesNotContain("010-1234-5678", "sk-example-key");
    }

    @Test
    void advisorsRunInNestedOrder() {
        List<String> steps = new ArrayList<>();
        new AdvisorOrderLesson()
                .traceOrder(ChatClient.create(new ScriptedChatModel()), "선물", steps);
        assertThat(steps).containsExactly("A:before", "B:before", "B:after", "A:after");
    }
}
