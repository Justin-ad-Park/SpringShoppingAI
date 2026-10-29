package com.example.shopping.tutor.lesson.l08etl;

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
class DocumentEtlLessonTest {

    @Test
    void readersAndSplitterPreserveSource() {
        var lesson = new DocumentEtlLesson();
        assertThat(lesson.readPolicyDocuments().data().toString()).contains("7일");
        var split = lesson.splitAndAttachMetadata();
        assertThat(((Number) split.data().get("count")).intValue()).isGreaterThan(1);
        assertThat(split.data().toString()).contains("source", "version");
    }
}
