package com.example.shopping.tutor.lesson.l07vector;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l04memory.*;
import com.example.shopping.tutor.lesson.l06tools.*;
import com.example.shopping.tutor.lesson.l06tools.support.DemoProductCatalog;
import com.example.shopping.tutor.lesson.l07vector.support.FixedEmbeddingModel;

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
class VectorStoreLessonTest {
    @Test
    void cosineAndDimensionsAreValidated() {
        assertThat(
                        com.example.shopping.tutor.lesson.l07vector.EmbeddingLesson.cosine(
                                new float[] {1, 0}, new float[] {1, 0}))
                .isEqualTo(1);
        assertThat(
                        com.example.shopping.tutor.lesson.l07vector.EmbeddingLesson.cosine(
                                new float[] {1, 0}, new float[] {0, 1}))
                .isZero();
        assertThatThrownBy(
                        () ->
                                com.example.shopping.tutor.lesson.l07vector.EmbeddingLesson.cosine(
                                        new float[] {1}, new float[] {1, 0}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void vectorMetadataFilterKeepsOnlyMatchingOriginalProduct() {
        var result = new VectorStoreLesson().filterMetadata("보온용 컵", new FixedEmbeddingModel(), 3);
        assertThat(result.data().get("products"))
                .isEqualTo(List.of(DemoProductCatalog.findProduct("P009")));
    }
}
