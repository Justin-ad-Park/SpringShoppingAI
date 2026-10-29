package com.example.shopping.tutor.lesson.l04memory;

import static com.example.shopping.tutor.platform.support.LessonExecutionFixture.context;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;
import com.example.shopping.tutor.lesson.l06tools.*;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.stub.ScriptedChatModel;
import com.example.shopping.tutor.platform.web.adapter.MemoryCases;

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
class ConversationMemoryLessonTest {
    @Test
    void memoryCarriesHistoryOnlyAfterCommitAndDoesNotCrossContexts() {
        var state = new LessonExecution.State();
        var first = context("예산은 3만원", "normal", state, new ScriptedChatModel());
        new MemoryCases().isolateAndReset(first);
        first.finish(state);
        assertThat(state.messages).hasSize(2);
        var second = context("그 예산으로", "normal", state, new ScriptedChatModel());
        new MemoryCases().isolateAndReset(second);
        assertThat(second.prompts.toString()).contains("예산은 3만원");
        var other = context("다른 세션");
        new MemoryCases().isolateAndReset(other);
        assertThat(other.prompts.toString()).doesNotContain("예산은 3만원");
    }

    @Test
    void clearAndKeepAreDifferent() {
        var old = new SearchCriteria(null, "음료", 30000L);
        var none = new SearchCriteria(null, null, null);
        assertThat(
                        com.example.shopping.tutor.lesson.l04memory.SearchCriteriaReducer.merge(
                                old, none, "예산 해제"))
                .isEqualTo(new SearchCriteria(null, "음료", null));
        assertThat(
                        com.example.shopping.tutor.lesson.l04memory.SearchCriteriaReducer.merge(
                                old, none, "계속 찾아줘"))
                .isEqualTo(old);
    }

    @Test
    void memoryLessonCanBeUsedWithoutPlatform() {
        var memory = MessageWindowChatMemory.builder().build();
        var lesson = new ConversationMemoryLesson(new ScriptedChatModel(), memory);
        lesson.chatWithMemory("a", "나는 음료를 찾고 있어");
        lesson.chatWithMemory("a", "계속 찾아줘");
        assertThat(memory.get("a")).hasSize(4);
        assertThat(memory.get("b")).isEmpty();
        assertThat(new MessageWindowLesson().keepRecentMessages(4)).hasSize(4);
        assertThat(new JdbcMemoryLesson().persistAndRestore("저장한 질문").getFirst().getText())
                .isEqualTo("저장한 질문");
    }
}
