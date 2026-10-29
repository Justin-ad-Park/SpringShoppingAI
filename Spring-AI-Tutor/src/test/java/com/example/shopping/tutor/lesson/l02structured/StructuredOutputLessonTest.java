package com.example.shopping.tutor.lesson.l02structured;

import static com.example.shopping.tutor.platform.support.LessonExecutionFixture.context;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l04memory.*;
import com.example.shopping.tutor.lesson.l06tools.*;
import com.example.shopping.tutor.lesson.l06tools.support.DemoProductCatalog;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.stub.ScriptedChatModel;
import com.example.shopping.tutor.platform.web.adapter.StructuredOutputCases;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
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
class StructuredOutputLessonTest {
    @Test
    void criteriaRejectsNegativeAndUnknownCategory() {
        assertThatThrownBy(() -> new SearchCriteria(null, null, -1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SearchCriteria(null, "없는분류", 100L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(DemoProductCatalog.search(new SearchCriteria(null, "음료", 9900L)))
                .containsExactly(DemoProductCatalog.findProduct("P004"));
    }

    @Test
    void entityConversionAndOneCorrectionUseActualChatClient() {
        var c =
                context(
                        "3만원 이하 음료 찾아줘",
                        "invalid-json",
                        new LessonExecution.State(),
                        new ScriptedChatModel());
        var result = new StructuredOutputCases().handleInvalidOutput(c);
        assertThat(result.data()).containsEntry("corrected", true);
        assertThat(c.prompts).hasSize(1);
    }

    @Test
    void invalidRecommendationsAreNotMadeIntoProductCards() {
        assertThatThrownBy(
                        () ->
                                com.example.shopping.tutor.lesson.l02structured
                                        .StructuredOutputLesson.validateRecommendations(
                                        List.of(new Recommendation("P999", "가짜"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                com.example.shopping.tutor.lesson.l02structured
                                        .StructuredOutputLesson.validateRecommendations(
                                        List.of(
                                                new Recommendation("P004", "a"),
                                                new Recommendation("P004", "b"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({"1만5천원 이하 음료,15000", "3천원 이하 음료,3000", "30000원 이하 음료,30000"})
    void scriptedBudgetMatchesPublishedShoppingSamples(String question, long expected) {
        assertThat(new StructuredOutputCases().extract(context(question)).maxPrice())
                .isEqualTo(expected);
    }

    @Test
    void nativeAndMultimodalNeverPretendToBeLiveInStub() {
        assertThat(new StructuredOutputCases().compareNativeSchema(context("검색")).status())
                .isEqualTo("UNSUPPORTED");
    }

    @Test
    void extractReturnsValidatedRecord() {
        var criteria =
                new StructuredOutputLesson()
                        .extract(ChatClient.create(new ScriptedChatModel()), "3만원 이하 생활용품");
        assertThat(criteria).isEqualTo(new SearchCriteria(null, "생활", 30000L));
    }
}
