package com.example.shopping.integration.support;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/** 쿠키를 보관하는 브라우저 세션. 테스트마다 새 객체를 만들어 상태를 격리한다. */
public final class ChatClient implements AutoCloseable {
    static final ObjectMapper JSON = new ObjectMapper();
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public ChatClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public JsonNode status() throws Exception {
        var response = http.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/status"))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body());
    }

    public Conversation conversation() {
        return conversation(UUID.randomUUID().toString());
    }

    public Conversation conversation(String id) {
        return new Conversation(id);
    }

    /** 버전은 내부에서 관리하며, 오류 검증 시 오래된 버전을 직접 지정할 수 있다. */
    public final class Conversation {
        private final String id;
        private int revision;

        private Conversation(String id) {
            this.id = id;
        }

        public JsonNode say(String message) throws Exception {
            return send(message, "standard", false);
        }

        public JsonNode send(String message, String scenario, boolean newSearch) throws Exception {
            var response = request(id, revision, message, scenario, newSearch);
            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
            revision++;
            return JSON.readTree(response.body());
        }

        public int requestStatus(String message, String scenario, int expectedRevision) throws Exception {
            return request(id, expectedRevision, message, scenario, false).statusCode();
        }
    }

    private HttpResponse<String> request(String id, int revision, String message,
                                         String scenario, boolean newSearch) throws Exception {
        var body = JSON.createObjectNode()
                .put("message", message)
                .put("scenario", scenario)
                .put("conversationId", id)
                .put("expectedRevision", revision)
                .put("newSearch", newSearch);

        var request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat"))
                .timeout(Duration.ofSeconds(180))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());

        // 서비스 간 전달 과정에서 같은 헤더가 중복 추가되지 않았는지도 확인한다.
        assertThat(response.headers().allValues("Transfer-Encoding")).hasSizeLessThanOrEqualTo(1);
        return response;
    }

    @Override
    public void close() {
        http.close();
    }

    public static List<String> ids(JsonNode result) {
        return StreamSupport.stream(result.path("products").spliterator(), false)
                .map(product -> product.path("id").asText()).toList();
    }

    public static List<String> tools(JsonNode result) {
        return StreamSupport.stream(result.path("toolCalls").spliterator(), false)
                .map(call -> call.path("tool").asText()).toList();
    }
}
