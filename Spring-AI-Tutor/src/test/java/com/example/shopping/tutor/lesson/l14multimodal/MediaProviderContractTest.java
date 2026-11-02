package com.example.shopping.tutor.lesson.l14multimodal;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l07vector.support.FixedEmbeddingModel;
import com.example.shopping.tutor.platform.config.AiModelConfiguration;
import com.example.shopping.tutor.platform.config.MediaModelConfiguration;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.stub.ScriptedChatModel;
import com.example.shopping.tutor.platform.web.adapter.ModerationCases;
import com.example.shopping.tutor.platform.web.adapter.MultimodalCases;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.*;
import org.springframework.core.io.ByteArrayResource;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 유료 미디어 생성을 호출하지 않고 실제 Spring AI 이미지·음성·moderation adapter를 검증한다. */
class MediaProviderContractTest {
    HttpServer server;
    MediaModelConfiguration models;
    tools.jackson.databind.JsonNode imageRequest;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/v1/images/generations",
                x -> {
                    imageRequest =
                            new tools.jackson.databind.ObjectMapper()
                                    .readTree(x.getRequestBody().readAllBytes());
                    reply(
                            x,
                            "application/json",
                            "{\"created\":1,\"data\":[{\"b64_json\":\"aW1hZ2UtZml4dHVyZQ==\"}]}"
                                    .getBytes(StandardCharsets.UTF_8));
                });
        server.createContext(
                "/v1/audio/transcriptions",
                x ->
                        reply(
                                x,
                                "application/json",
                                "{\"text\":\"3만원 이하 텀블러 찾아줘\"}".getBytes(StandardCharsets.UTF_8)));
        server.createContext(
                "/v1/audio/speech",
                x -> reply(x, "audio/mpeg", "audio-fixture".getBytes(StandardCharsets.UTF_8)));
        server.createContext(
                "/v1/moderations",
                x ->
                        reply(
                                x,
                                "application/json",
                                moderationResponse()
                                        .getBytes(StandardCharsets.UTF_8)));
        server.start();
        models =
                new MediaModelConfiguration(
                        "contract-placeholder",
                        "http://127.0.0.1:" + server.getAddress().getPort(),
                        "contract-image",
                        "contract-transcription",
                        "contract-speech",
                        "contract-moderation");
    }

    String moderationResponse() {
        var mapper = new tools.jackson.databind.ObjectMapper();
        var root = mapper.createObjectNode().put("id", "mod").put("model", "contract");
        var result = root.putArray("results").addObject().put("flagged", false);
        var categories = result.putObject("categories");
        var scores = result.putObject("category_scores");
        for (String name : List.of("sexual", "sexual/minors", "harassment", "harassment/threatening",
                "hate", "hate/threatening", "illicit", "illicit/violent", "self-harm",
                "self-harm/intent", "self-harm/instructions", "violence", "violence/graphic")) {
            categories.put(name, false);
            scores.put(name, 0.0);
        }
        return root.toString();
    }

    void reply(com.sun.net.httpserver.HttpExchange x, String type, byte[] bytes)
            throws java.io.IOException {
        x.getRequestBody().readAllBytes();
        x.getResponseHeaders().set("Content-Type", type);
        x.sendResponseHeaders(200, bytes.length);
        try (var out = x.getResponseBody()) {
            out.write(bytes);
        }
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    LessonExecution context(String message) {
        return new LessonExecution(
                message,
                "standard",
                "normal",
                "media",
                Map.of(),
                new AiModelConfiguration.Settings(
                        "live", "contract", "fixed", "http://127.0.0.1:1", false),
                new ScriptedChatModel(),
                new FixedEmbeddingModel(),
                new LessonExecution.State(),
                null);
    }

    @Test
    void L1402_transcriptionUsesMultipartAdapterAndDoesNotExecuteShoppingAction() {
        var c = context("전사");
        c.inputMime = "audio/wav";
        c.inputFile =
                new ByteArrayResource(new byte[44]) {
                    public String getFilename() {
                        return "input.wav";
                    }
                };
        var out = new MultimodalCases(models).transcribeShoppingRequest(c);
        assertThat(out.answer()).contains("3만원");
        assertThat(out.data()).containsEntry("automaticExecution", false);
    }

    @Test
    void L1403_speechReturnsPlayableContentContract() {
        var out = new MultimodalCases(models).speakRecommendation(context("허브티 추천"));
        assertThat(out.data()).containsEntry("contentType", "audio/mpeg");
        assertThat(Base64.getDecoder().decode((String) out.data().get("audioBase64")))
                .isEqualTo("audio-fixture".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void L1404_imageUsesImageModelAndLabelsSyntheticResult() {
        var out = new MultimodalCases(models).generateGiftPreview(context("가상 포장"));
        assertThat(out.answer()).contains("AI 생성");
        assertThat(out.data()).containsKey("imageBase64");
        assertThat(imageRequest.hasNonNull("response_format")).isFalse();
    }

    @Test
    void L1405_moderationUsesSeparateApiAndReturnsVerdict() {
        var out = new ModerationCases(models).checkAbusiveMessage(context("일반 문의"));
        assertThat(out.data().get("moderation")).isNotNull();
    }

    @Test
    void missingOrWrongMediaCannotReachModel() {
        assertThatThrownBy(
                        () -> new MultimodalCases(models).transcribeShoppingRequest(context("전사")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MultimodalCases(models).searchFromProductImage(context("이미지")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
