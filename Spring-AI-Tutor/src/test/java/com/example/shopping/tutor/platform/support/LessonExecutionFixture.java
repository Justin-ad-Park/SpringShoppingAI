package com.example.shopping.tutor.platform.support;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l07vector.support.FixedEmbeddingModel;
import com.example.shopping.tutor.platform.config.AiModelConfiguration;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.stub.ScriptedChatModel;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.ai.chat.memory.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public final class LessonExecutionFixture {
    public static LessonExecution context(
            String message, String experiment, LessonExecution.State state, ChatModel model) {
        return new LessonExecution(
                message,
                "standard",
                experiment,
                "conversation",
                Map.of(),
                new AiModelConfiguration.Settings(
                        "stub", "scripted", "fixed", "http://127.0.0.1:1", false),
                model,
                new FixedEmbeddingModel(),
                state,
                null);
    }

    public static LessonExecution context(String message) {
        return context(message, "normal", new LessonExecution.State(), new ScriptedChatModel());
    }
}
