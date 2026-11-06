package com.example.ailearning.testsupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantTextMessage;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantToolCallMessage;
import static org.junit.jupiter.api.Assertions.*;

class StubServerTest {
    private final TestOutput output = new TestOutput();
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test void 텍스트와_usage와_인증_기록을_제공한다() throws Exception {
        try (var model = new OpenAiStubServer()) {
            model
                    .chat()
                    .setResponseMessage(createAssistantTextMessage("고정 응답"));
            model
                    .chat()
                    .setTokenUsage(10, 5);
            var response = post(model.getBaseUrl() + "/chat/completions", "{\"model\":\"test-model\"}");
            output.printHttpResponse(response);
            var body = OpenAiStubServer.JSON.readTree(response.body());
            assertEquals("고정 응답", body
                            .at("/choices/0/message/content")
                            .asString());
            assertEquals(15, body
                            .at("/usage/total_tokens")
                            .asInt());
            assertEquals("test-model", body
                            .path("model")
                            .asString());
            assertEquals(java.util.List.of("Bearer local-secret"), model
                            .chat()
                            .getAuthorizationHeaders());
        }
    }
    @Test void 도구_요청과_답변을_순서대로_반환하고_소진되면_실패한다() throws Exception {
        try (var model = new OpenAiStubServer()) {
            model
                    .chat()
                    .setResponseSequence(createAssistantToolCallMessage("call-1", "search", "{}"), createAssistantTextMessage("최종 답변"));
            var first = output.printHttpResponse(post(model.getBaseUrl() + "/chat/completions", "{}"));
            var next = output.printHttpResponse(post(model.getBaseUrl() + "/chat/completions", "{}"));
            var exhausted = output.printHttpResponse(post(model.getBaseUrl() + "/chat/completions", "{}"));
            assertTrue(first
                            .body()
                            .contains("call-1"));
            assertTrue(first
                            .body()
                            .contains("tool_calls"));
            assertTrue(next
                            .body()
                            .contains("최종 답변"));
            assertEquals(500, exhausted.statusCode());
            assertTrue(exhausted
                            .body()
                            .contains("stub_response_not_configured"));
            assertEquals(3, model
                            .chat()
                            .getRequests()
                            .size());
        }
    }
    @Test void 임베딩_배열과_시작_기록은_채팅_초기화_후에도_유지한다() throws Exception {
        try (var model = new OpenAiStubServer()) {
            model
                    .embeddings()
                    .setVectorFunction(value -> value.equals("배송") ? new float[]{1, 0} : new float[]{0, 1});
            var response = output.printHttpResponse(post(model.getBaseUrl() + "/embeddings", "{\"input\":[\"배송\",\"반품\"]}"));
            var body = OpenAiStubServer.JSON.readTree(response.body());
            assertEquals(2, body
                            .path("data")
                            .size());
            assertEquals(1, body
                            .at("/data/0/embedding/0")
                            .asInt());
            model
                    .chat()
                    .setResponseMessage(createAssistantTextMessage("이전 설정"));
            post(model.getBaseUrl() + "/chat/completions", "{}");
            model
                    .chat()
                    .resetResponsesAndRequests();
            assertEquals(1, model
                            .embeddings()
                            .getRequests()
                            .size());
            assertTrue(model
                            .chat()
                            .getRequests()
                            .isEmpty());
            assertEquals(500, output
                            .printHttpResponse(post(model.getBaseUrl() + "/chat/completions", "{}"))
                            .statusCode());
            assertEquals(200, output
                            .printHttpResponse(post(model.getBaseUrl() + "/embeddings", "{\"input\":\"배송\"}"))
                            .statusCode());
        }
    }
    @Test void 오류_요청도_기록하고_초기화하면_오류와_지연이_해제된다() throws Exception {
        try (var model = new OpenAiStubServer()) {
            model
                    .chat()
                    .setHttpStatus(429);
            model
                    .chat()
                    .setResponseDelay(Duration.ofMillis(5));
            var response = output.printHttpResponse(post(model.getBaseUrl() + "/chat/completions", "{}"));
            assertEquals(429, response.statusCode());
            assertEquals(1, model
                            .chat()
                            .getRequests()
                            .size());
            model
                    .chat()
                    .resetResponsesAndRequests();
            model
                    .chat()
                    .setResponseMessage(createAssistantTextMessage("다음 요청"));
            assertEquals(200, output
                            .printHttpResponse(post(model.getBaseUrl() + "/chat/completions", "{}"))
                            .statusCode());
            assertEquals(1, model
                            .chat()
                            .getRequests()
                            .size());
        }
    }
    @Test void 지연_중_설정을_바꿔도_이미_접수한_응답은_바뀌지_않는다() throws Exception {
        var captured = new CountDownLatch(1);
        try (var model = new OpenAiStubServer(); var client = HttpClient.newHttpClient()) {
            model
                    .chat()
                    .setResponseDelay(Duration.ofMillis(150));
            model
                    .chat()
                    .setResponseFunction(request -> { captured.countDown(); return createAssistantTextMessage("처음 설정"); });
            var future = client.sendAsync(request(model.getBaseUrl() + "/chat/completions", "{}"), HttpResponse.BodyHandlers.ofString());
            assertTrue(captured.await(3, TimeUnit.SECONDS));
            model
                    .chat()
                    .setResponseMessage(createAssistantTextMessage("나중 설정"));
            var first = output.printHttpResponse(future.get(3, TimeUnit.SECONDS));
            assertTrue(first
                            .body()
                            .contains("처음 설정"));
            assertTrue(output
                            .printHttpResponse(post(model.getBaseUrl() + "/chat/completions", "{}"))
                            .body()
                            .contains("나중 설정"));
        }
    }
    @Test void 서버별로_포트와_요청_기록을_격리한다() throws Exception {
        try (var alice = new OpenAiStubServer(); var bob = new OpenAiStubServer()) {
            alice
                    .chat()
                    .setResponseMessage(createAssistantTextMessage("Alice"));
            bob
                    .chat()
                    .setResponseMessage(createAssistantTextMessage("Bob"));
            assertNotEquals(alice.getBaseUrl(), bob.getBaseUrl());
            assertTrue(output
                            .printHttpResponse(post(alice.getBaseUrl() + "/chat/completions", "{}"))
                            .body()
                            .contains("Alice"));
            assertTrue(bob
                            .chat()
                            .getRequests()
                            .isEmpty());
            assertTrue(output
                            .printHttpResponse(post(bob.getBaseUrl() + "/chat/completions", "{}"))
                            .body()
                            .contains("Bob"));
            assertEquals(1, alice
                            .chat()
                            .getRequests()
                            .size());
        }
    }
    @Test void Anthropic은_별도_응답_형식과_인증_기록을_사용한다() throws Exception {
        try (var model = new AnthropicStubServer(); var client = HttpClient.newHttpClient()) {
            model.setMessageResponseText("Anthropic 응답");
            var request = HttpRequest.newBuilder(URI.create(model.getBaseUrl() + "/v1/messages"))
                    .header("x-api-key", "local-anthropic")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"model\":\"test\"}"))
                    .build();
            var response = output.printHttpResponse(client.send(request, HttpResponse.BodyHandlers.ofString()));
            var body = OpenAiStubServer.JSON.readTree(response.body());
            assertEquals("message", body
                            .path("type")
                            .asString());
            assertEquals("Anthropic 응답", body
                            .at("/content/0/text")
                            .asString());
            assertEquals(java.util.List.of("local-anthropic"), model.getMessageApiKeyHeaders());
            model.resetMessageResponseAndRequests();
            assertTrue(model
                            .getMessageRequests()
                            .isEmpty());
            assertEquals(500, output
                            .printHttpResponse(post(model.getBaseUrl() + "/v1/messages", "{}"))
                            .statusCode());
        }
    }
    @Test void 응답_생성_중_usage를_변경해도_현재_요청에는_기존_설정을_쓴다() throws Exception {
        var started = new CountDownLatch(1);
        var resume = new CountDownLatch(1);
        try (var model = new OpenAiStubServer(); var client = HttpClient.newHttpClient()) {
            model
                    .chat()
                    .setTokenUsage(10, 5);
            model
                    .chat()
                    .setResponseFunction(request -> {
                        started.countDown();
                        try {
                            if (!resume.await(3, TimeUnit.SECONDS)) throw new IllegalStateException("응답 대기 시간 초과");
                        } catch (InterruptedException error) {
                            Thread.currentThread()
                                    .interrupt();
                            throw new IllegalStateException(error);
                        }
                        return createAssistantTextMessage("설정 스냅샷 확인");
                    });
            var future = client.sendAsync(request(model.getBaseUrl() + "/chat/completions", "{}"), HttpResponse.BodyHandlers.ofString());
            try {
                assertTrue(started.await(3, TimeUnit.SECONDS));
                model
                        .chat()
                        .setTokenUsage(100, 50);
            } finally {
                resume.countDown();
            }
            var first = output.printHttpResponse(future.get(3, TimeUnit.SECONDS));
            var next = output.printHttpResponse(post(model.getBaseUrl() + "/chat/completions", "{}"));
            assertEquals(15, OpenAiStubServer.JSON
                            .readTree(first.body())
                            .at("/usage/total_tokens")
                            .asInt());
            assertEquals(150, OpenAiStubServer.JSON
                            .readTree(next.body())
                            .at("/usage/total_tokens")
                            .asInt());
        }
    }

    @Test void 초기화_후에도_usage_설정은_유지한다() throws Exception {
        try (var model = new OpenAiStubServer()) {
            model
                    .chat()
                    .setTokenUsage(10, 5);
            model
                    .chat()
                    .setResponseMessage(createAssistantTextMessage("이전 응답"));
            model
                    .chat()
                    .resetResponsesAndRequests();
            model
                    .chat()
                    .setResponseMessage(createAssistantTextMessage("새 응답"));
            var response = output.printHttpResponse(post(model.getBaseUrl() + "/chat/completions", "{}"));
            assertEquals(15, OpenAiStubServer.JSON
                            .readTree(response.body())
                            .at("/usage/total_tokens")
                            .asInt());
        }
    }

    @Test void 지정한_출력에_결과를_쓰고_인증_헤더는_포함하지_않는다() throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        try (var stream = new java.io.PrintStream(bytes, true, java.nio.charset.StandardCharsets.UTF_8);
             var model = new OpenAiStubServer()) {
            model
                    .chat()
                    .setResponseMessage(createAssistantTextMessage("출력할 응답"));
            var captured = new TestOutput(stream);
            var response = post(model.getBaseUrl() + "/chat/completions", "{}");
            assertSame(response, captured.printHttpResponse(response));
            assertSame(response, captured.printHttpResponse("두 번째 표시", response));
            String printed = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
            output.printValue("캡처한 출력", printed);
            assertTrue(printed.startsWith("요청: POST /v1/chat/completions"));
            assertFalse(printed.contains("===== ["));
            assertTrue(printed.contains("두 번째 표시 / HTTP 200"));
            assertTrue(printed.contains("출력할 응답"));
            assertFalse(printed.contains("local-secret"));
            assertFalse(printed.contains("Authorization"));
        }
    }

    @Test void 임베딩_콜백이_배열을_재사용해도_입력별_값을_보존한다() throws Exception {
        float[] reused = new float[1];
        try (var model = new OpenAiStubServer()) {
            model
                    .embeddings()
                    .setVectorFunction(value -> {
                        reused[0] = value.equals("배송") ? 1 : 2;
                        return reused;
                    });
            var response = output.printHttpResponse(post(model.getBaseUrl() + "/embeddings", "{\"input\":[\"배송\",\"반품\"]}"));
            var body = OpenAiStubServer.JSON.readTree(response.body());
            assertEquals(1, body
                            .at("/data/0/embedding/0")
                            .asInt());
            assertEquals(2, body
                            .at("/data/1/embedding/0")
                            .asInt());
        }
    }

    HttpRequest request(String url, String json) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer local-secret")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }
    HttpResponse<String> post(String url, String json) throws Exception {
        try (var client = HttpClient.newHttpClient()) { return client.send(request(url, json), HttpResponse.BodyHandlers.ofString()); }
    }
}
