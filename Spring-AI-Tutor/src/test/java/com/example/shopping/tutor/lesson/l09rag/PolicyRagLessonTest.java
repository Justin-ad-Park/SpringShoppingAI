package com.example.shopping.tutor.lesson.l09rag;

import static com.example.shopping.tutor.platform.support.LessonExecutionFixture.context;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l04memory.*;
import com.example.shopping.tutor.lesson.l06tools.*;
import com.example.shopping.tutor.lesson.l07vector.support.FixedEmbeddingModel;
import com.example.shopping.tutor.platform.web.adapter.RagCases;

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
class PolicyRagLessonTest {
    @Test
    void ragFiltersOtherMallAndOldPolicyAndHandlesMissing() {
        var c = context("미개봉 반품 조건");
        var result = new RagCases().answerWithManualContext(c);
        assertThat(result.data().toString())
                .contains("return-current")
                .doesNotContain("return-other", "return-old");
        var unknown = context("해외 관세");
        assertThat(new RagCases().handleMissingEvidence(unknown).status())
                .isEqualTo("INSUFFICIENT_EVIDENCE");
        assertThat(unknown.prompts).isEmpty();
    }

    @Test
    void retrievalFiltersAreEnforcedBeforeModelCall() {
        var docs = new PolicyRagLesson().retrieve("normal", new FixedEmbeddingModel(), 3, "반품");
        assertThat(docs).isNotEmpty();
        assertThat(docs)
                .allSatisfy(
                        doc -> {
                            assertThat(doc.getMetadata())
                                    .containsEntry("mallId", "TUTOR")
                                    .containsEntry("current", true);
                        });
    }
}
