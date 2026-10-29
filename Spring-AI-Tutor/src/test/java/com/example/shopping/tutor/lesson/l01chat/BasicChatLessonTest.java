package com.example.shopping.tutor.lesson.l01chat;

import static com.example.shopping.tutor.platform.support.LessonExecutionFixture.context;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l04memory.*;
import com.example.shopping.tutor.lesson.l06tools.*;
import com.example.shopping.tutor.platform.stub.ScriptedChatModel;
import com.example.shopping.tutor.platform.web.adapter.PromptCases;

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
class BasicChatLessonTest {
    @Test
    void bracesInUserInputRemainLiteral() {
        var c = context("{question} {unknown} 포함 질문");
        new PromptCases().renderTemplate(c);
        assertThat(c.prompts.toString()).contains("{question}", "{unknown}");
        assertThatThrownBy(() -> new PromptTemplate("{missing}").render(Map.of()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void rolePromptAndOptionsAreVisibleWithoutWebContext() {
        var lesson = new BasicChatLesson(new ScriptedChatModel());
        assertThat(lesson.separateRoles("재고 알려줘")).isNotBlank();
        assertThatThrownBy(() -> lesson.inspectOptionsAndUsage("질문", Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
