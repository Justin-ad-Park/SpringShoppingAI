package com.example.ailearning;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 Spring AI 어댑터를 사용하되 외부 모델 API만 로컬 HTTP 서버로 대체합니다.
// 02장에서는 modelProperties와 정상 호출 테스트만 읽고, 서버 구현은 제공된 보조 코드로 사용하세요.
// RANDOM_PORT는 테스트할 애플리케이션 서버에 사용 가능한 포트를 배정합니다.
@DisplayName("첫 Chat 호출 — 로컬 MockModel 계약 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class FirstChatLocalMockModelTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    // 가짜 서버의 처리 스레드와 테스트 스레드가 요청 기록을 공유합니다.
    private static final List<JsonNode> REQUESTS = new CopyOnWriteArrayList<>();
    private static final List<String> AUTH_HEADERS = new CopyOnWriteArrayList<>();
    private static final HttpServer MODEL_SERVER = startModelServer();  // (1). static 변수를 담는 로직 자동 수행됨

    @Value("${local.server.port}")
    private int port;

    // 테스트에서만 적용하며 application.yml의 같은 설정을 덮어씁니다.
    @DynamicPropertySource
    static void modelProperties(DynamicPropertyRegistry registry) {
        // 실제 키 대신 더미 키를 사용합니다. 아래 인증 헤더 검증에도 쓰이므로 유지합니다.
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        // 실제 모델 대신 요청에 담길 검증용 모델 이름입니다.
        registry.add("spring.ai.openai.chat.options.model", () -> "tutor-test-model");
        // 외부 제공 업체 대신 로컬 가짜 모델 서버로 요청을 보냅니다.
        registry.add("spring.ai.openai.base-url",
                () -> "http://127.0.0.1:" + MODEL_SERVER.getAddress().getPort() + "/v1");
    }


    // 각 테스트 전에 기록을 비워 다른 테스트의 요청이 섞이지 않게 합니다.
    @BeforeEach
    void resetRequests() {
        REQUESTS.clear();
        AUTH_HEADERS.clear();


    }

    // 모든 테스트가 끝나면 가짜 모델 서버를 종료합니다.
    @AfterAll
    static void stopModelServer() {
        MODEL_SERVER.stop(0);
    }

    // 02장 핵심: 질문 전달과 고정 응답 반환을 확인합니다.
    @Test
    @DisplayName("로컬 MockModel에 질문을 전달하고 고정 응답을 반환한다")
    void sendsUserMessageToLocalMockModelAndReturnsFixedText() throws Exception {
        HttpResponse<String> response = ask("스프링이 무엇인가요?");
        printResponse("LocalMockModel / 정상 호출 / 정상 질문", response);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("로컬 테스트 모델의 고정 응답입니다.");
        assertThat(REQUESTS).hasSize(1);
        JsonNode request = REQUESTS.getFirst();
        assertThat(request.path("model").asText()).isEqualTo("tutor-test-model");
        assertThat(request.path("messages").size()).isEqualTo(1);
        assertThat(request.at("/messages/0/role").asText()).isEqualTo("user");
        assertThat(request.at("/messages/0/content").asText()).isEqualTo("스프링이 무엇인가요?");
        // 선택 심화: 더미 키가 제공 업체 요청의 인증 헤더에 들어가는지도 확인합니다.
        assertThat(AUTH_HEADERS).containsExactly("Bearer local-test-key");
    }

    /**
     * 02장 보충: Spring MVC/Validation이 모델 호출 전에 잘못된 입력을 거부하는지 확인합니다.
     * https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/support/DefaultHandlerExceptionResolver.html
     * @throws Exception
     */
    @Test
    @DisplayName("잘못된 입력은 로컬 MockModel 호출 전에 거부한다")
    void rejectsInvalidInputsBeforeCallingLocalMockModel() throws Exception {
        HttpResponse<String> blankResponse = ask("   ");
        printResponse("LocalMockModel / 입력 검증 / 공백 입력", blankResponse);
        assertThat(blankResponse.statusCode()).isEqualTo(400);

        HttpResponse<String> oversizedResponse = ask("가".repeat(ChatController.MAX_MESSAGE_LENGTH + 1));
        printResponse("LocalMockModel / 입력 검증 / 최대 길이보다 한 글자 긴 입력", oversizedResponse);
        assertThat(oversizedResponse.statusCode()).isEqualTo(400);
        assertThat(REQUESTS).isEmpty();
    }

    // 03장에서 읽기: 두 번째 요청에 현재 질문만 담기는지 확인합니다.
    // 실제 모델의 기억 능력을 평가하는 테스트는 아닙니다.
    @Test
    @DisplayName("로컬 MockModel의 다음 요청에 이전 대화를 자동 포함하지 않는다")
    void doesNotSendPreviousRequestToLocalMockModelAutomatically() throws Exception {
        HttpResponse<String> firstResponse = ask("제 이름은 민수입니다.");
        printResponse("LocalMockModel / 대화 전달 / 첫 번째 요청", firstResponse);
        assertThat(firstResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> secondResponse = ask("제 이름이 무엇인가요?");
        printResponse("LocalMockModel / 대화 전달 / 두 번째 요청", secondResponse);
        assertThat(secondResponse.statusCode()).isEqualTo(200);

        assertThat(REQUESTS).hasSize(2);
        JsonNode second = REQUESTS.get(1);
        assertThat(second.path("messages").size()).isEqualTo(1);
        assertThat(second.at("/messages/0/content").asText()).isEqualTo("제 이름이 무엇인가요?");
    }

    // curl 대신 애플리케이션의 /api/chat으로 HTTP 요청을 보내는 보조 메서드입니다.
    private HttpResponse<String> ask(String message) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/chat"))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(message))
                .build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    // 각 테스트에서 테스트 이름과 요청 구분을 전달해 결과를 출력합니다.
    private void printResponse(String label, HttpResponse<String> response) {
        System.out.println("=== " + label + " ===");
        System.out.println("응답 상태 코드: " + response.statusCode());
        System.out.println("응답 본문: " + response.body());
    }

    // 제공된 테스트 보조 코드입니다. 첫 실습에서는 내부 구현을 이해하거나 작성할 필요가 없습니다.
    private static HttpServer startModelServer() {
        try {
            // 포트 0은 가짜 모델 서버에 사용 가능한 포트를 배정하도록 요청합니다.
            // 위 RANDOM_PORT로 실행되는 애플리케이션 서버와는 별개의 서버입니다.
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v1/chat/completions", exchange -> {
                REQUESTS.add(JSON.readTree(exchange.getRequestBody()));
                AUTH_HEADERS.add(exchange.getRequestHeaders().getFirst("Authorization"));
                // Spring AI가 읽을 수 있도록 OpenAI 형식의 고정 JSON 응답을 반환합니다.
                byte[] response = """
                        {"id":"chatcmpl-local","object":"chat.completion","created":0,
                         "model":"tutor-test-model","choices":[{"index":0,
                         "message":{"role":"assistant","content":"로컬 테스트 모델의 고정 응답입니다."},
                         "finish_reason":"stop"}],
                         "usage":{"prompt_tokens":10,"completion_tokens":10,"total_tokens":20}}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                try (var body = exchange.getResponseBody()) {
                    body.write(response);
                }
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("로컬 테스트 모델 서버를 시작하지 못했습니다.", e);
        }
    }
}
