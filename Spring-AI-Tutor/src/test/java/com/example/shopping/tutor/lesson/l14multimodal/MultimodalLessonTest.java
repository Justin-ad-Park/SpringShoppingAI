package com.example.shopping.tutor.lesson.l14multimodal;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l04memory.*;
import com.example.shopping.tutor.lesson.l06tools.*;
import com.example.shopping.tutor.platform.stub.ScriptedChatModel;
import com.example.shopping.tutor.platform.web.MediaInput;

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
class MultimodalLessonTest {
    @Test
    void filesAreValidatedBeforeModelCalls() {
        assertThatThrownBy(() -> MediaInput.validate("L14-01", "image/png", new byte[20]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MediaInput.validate("L14-02", "audio/mpeg", new byte[20]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> MediaInput.validate("L14-01", "image/png", new byte[6 * 1024 * 1024]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void imageInputMustExistBeforeCallingProvider() {
        assertThatThrownBy(
                        () ->
                                new MultimodalLesson()
                                        .searchFromProductImage(
                                                ChatClient.create(new ScriptedChatModel()),
                                                new ScriptedChatModel(),
                                                "이미지",
                                                "stub",
                                                null,
                                                null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
