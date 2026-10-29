package com.example.shopping.tutor.lesson.l13shopping;

import static com.example.shopping.tutor.platform.support.LessonExecutionFixture.context;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;
import com.example.shopping.tutor.lesson.l04memory.*;
import com.example.shopping.tutor.lesson.l06tools.*;
import com.example.shopping.tutor.lesson.l06tools.support.DemoProductCatalog;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.stub.ScriptedChatModel;
import com.example.shopping.tutor.platform.web.adapter.ShoppingAssistantCases;

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
class ShoppingAssistantLessonTest {
    @Test
    void failedTurnCannotCommitStagedCriteriaOrMemory() {
        var state = new LessonExecution.State();
        state.criteria = new SearchCriteria(null, "생활", 30000L);
        var c = context("음료만", "timeout", state, new ScriptedChatModel());
        assertThatThrownBy(() -> new ShoppingAssistantCases().recoverAndReset(c))
                .isInstanceOf(RuntimeException.class);
        assertThat(state.criteria).isEqualTo(new SearchCriteria(null, "생활", 30000L));
        assertThat(state.messages).isEmpty();
    }

    @Test
    void zeroBudgetAndNoResultsDoNotResetCriteria() {
        var c = context("0원 이하 음료");
        var state = new LessonExecution.State();
        var outcome = new ShoppingAssistantCases().recommendWithFollowUp(c);
        c.finish(state);
        assertThat(outcome.status()).isEqualTo("NO_RESULTS");
        assertThat(state.criteria.maxPrice()).isZero();
    }

    @Test
    void recommendationKeepsOriginalCriteriaWhenQuestionOmitsIt() {
        var previous = new SearchCriteria(null, "음료", 10000L);
        var result =
                new ShoppingAssistantLesson()
                        .recommendWithFollowUp(
                                ChatClient.create(new ScriptedChatModel()), "계속 찾아줘", previous);
        assertThat(result.data()).containsEntry("criteria", previous);
        assertThat(result.data().get("products"))
                .isEqualTo(List.of(DemoProductCatalog.findProduct("P004")));
    }
}
