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
import static com.example.ailearning.testsupport.OpenAiStubServer.text;
import static com.example.ailearning.testsupport.OpenAiStubServer.toolCall;
import static org.junit.jupiter.api.Assertions.*;

class StubServerTest {
    private TestOutput output;
    @BeforeEach void 이름_준비(TestInfo info) { output = new TestOutput(info.getTestMethod().orElseThrow().getName()); }

    @Test void 텍스트와_usage와_인증_기록을_제공한다() throws Exception {
        try (var model = new OpenAiStubServer().respondWith(text("고정 응답")).usage(10, 5)) {
            var response = post(model.baseUrl() + "/chat/completions", "{\"model\":\"test-model\"}");
            output.response(response);
            var body = OpenAiStubServer.JSON.readTree(response.body());
            assertEquals("고정 응답", body.at("/choices/0/message/content").asString());
            assertEquals(15, body.at("/usage/total_tokens").asInt());
            assertEquals("test-model", body.path("model").asString());
            assertEquals(java.util.List.of("Bearer local-secret"), model.authorizationHeaders());
        }
    }
    @Test void 도구_요청과_답변을_순서대로_반환하고_소진되면_실패한다() throws Exception {
        try (var model = new OpenAiStubServer().respondInOrder(toolCall("call-1", "search", "{}"), text("최종 답변"))) {
            var first = output.response(post(model.baseUrl() + "/chat/completions", "{}"));
            var next = output.response(post(model.baseUrl() + "/chat/completions", "{}"));
            var exhausted = output.response(post(model.baseUrl() + "/chat/completions", "{}"));
            assertTrue(first.body().contains("call-1"));
            assertTrue(first.body().contains("tool_calls"));
            assertTrue(next.body().contains("최종 답변"));
            assertEquals(500, exhausted.statusCode());
            assertTrue(exhausted.body().contains("stub_response_not_configured"));
            assertEquals(3, model.chatRequests().size());
        }
    }
    @Test void 임베딩_배열과_시작_기록은_채팅_초기화_후에도_유지한다() throws Exception {
        try (var model = new OpenAiStubServer().embeddingUsing(value -> value.equals("배송") ? new float[]{1, 0} : new float[]{0, 1})) {
            var response = output.response(post(model.baseUrl() + "/embeddings", "{\"input\":[\"배송\",\"반품\"]}"));
            var body = OpenAiStubServer.JSON.readTree(response.body());
            assertEquals(2, body.path("data").size());
            assertEquals(1, body.at("/data/0/embedding/0").asInt());
            model.respondWith(text("이전 설정"));
            post(model.baseUrl() + "/chat/completions", "{}");
            model.resetChat();
            assertEquals(1, model.embeddingRequests().size());
            assertTrue(model.chatRequests().isEmpty());
            assertEquals(500, output.response(post(model.baseUrl() + "/chat/completions", "{}")).statusCode());
            assertEquals(200, output.response(post(model.baseUrl() + "/embeddings", "{\"input\":\"배송\"}")).statusCode());
        }
    }
    @Test void 오류_요청도_기록하고_초기화하면_오류와_지연이_해제된다() throws Exception {
        try (var model = new OpenAiStubServer().status(429).delay(Duration.ofMillis(5))) {
            var response = output.response(post(model.baseUrl() + "/chat/completions", "{}"));
            assertEquals(429, response.statusCode());
            assertEquals(1, model.chatRequests().size());
            model.resetChat();
            model.respondWith(text("다음 요청"));
            assertEquals(200, output.response(post(model.baseUrl() + "/chat/completions", "{}")).statusCode());
            assertEquals(1, model.chatRequests().size());
        }
    }
    @Test void 지연_중_설정을_바꿔도_이미_접수한_응답은_바뀌지_않는다() throws Exception {
        var captured = new CountDownLatch(1);
        try (var model = new OpenAiStubServer().delay(Duration.ofMillis(150)); var client = HttpClient.newHttpClient()) {
            model.respondUsing(request -> { captured.countDown(); return text("처음 설정"); });
            var future = client.sendAsync(request(model.baseUrl() + "/chat/completions", "{}"), HttpResponse.BodyHandlers.ofString());
            assertTrue(captured.await(3, TimeUnit.SECONDS));
            model.respondWith(text("나중 설정"));
            var first = output.response(future.get(3, TimeUnit.SECONDS));
            assertTrue(first.body().contains("처음 설정"));
            assertTrue(output.response(post(model.baseUrl() + "/chat/completions", "{}")).body().contains("나중 설정"));
        }
    }
    @Test void 서버별로_포트와_요청_기록을_격리한다() throws Exception {
        try (var alice = new OpenAiStubServer().respondWith(text("Alice")); var bob = new OpenAiStubServer().respondWith(text("Bob"))) {
            assertNotEquals(alice.baseUrl(), bob.baseUrl());
            assertTrue(output.response(post(alice.baseUrl() + "/chat/completions", "{}")).body().contains("Alice"));
            assertTrue(bob.chatRequests().isEmpty());
            assertTrue(output.response(post(bob.baseUrl() + "/chat/completions", "{}")).body().contains("Bob"));
            assertEquals(1, alice.chatRequests().size());
        }
    }
    @Test void Anthropic은_별도_응답_형식과_인증_기록을_사용한다() throws Exception {
        try (var model = new AnthropicStubServer(); var client = HttpClient.newHttpClient()) {
            model.respondText("Anthropic 응답");
            var request = HttpRequest.newBuilder(URI.create(model.baseUrl() + "/v1/messages"))
                    .header("x-api-key", "local-anthropic").POST(HttpRequest.BodyPublishers.ofString("{\"model\":\"test\"}")).build();
            var response = output.response(client.send(request, HttpResponse.BodyHandlers.ofString()));
            var body = OpenAiStubServer.JSON.readTree(response.body());
            assertEquals("message", body.path("type").asString());
            assertEquals("Anthropic 응답", body.at("/content/0/text").asString());
            assertEquals(java.util.List.of("local-anthropic"), model.apiKeyHeaders());
            model.reset();
            assertTrue(model.requests().isEmpty());
            assertEquals(500, output.response(post(model.baseUrl() + "/v1/messages", "{}")).statusCode());
        }
    }
    HttpRequest request(String url, String json) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json").header("Authorization", "Bearer local-secret")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
    }
    HttpResponse<String> post(String url, String json) throws Exception {
        try (var client = HttpClient.newHttpClient()) { return client.send(request(url, json), HttpResponse.BodyHandlers.ofString()); }
    }
}
