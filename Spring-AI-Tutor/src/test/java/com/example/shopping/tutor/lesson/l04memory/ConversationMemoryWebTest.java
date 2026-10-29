package com.example.shopping.tutor.lesson.l04memory;

import static com.example.shopping.tutor.platform.support.LessonExecutionFixture.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.stub.ScriptedChatModel;
import com.example.shopping.tutor.platform.web.adapter.MemoryCases;

import org.junit.jupiter.api.Test;

/** 웹 연결을 확인하는 추가 테스트. 처음에는 ConversationMemoryLessonTest만 읽어도 된다. */
class ConversationMemoryWebTest {
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
}
