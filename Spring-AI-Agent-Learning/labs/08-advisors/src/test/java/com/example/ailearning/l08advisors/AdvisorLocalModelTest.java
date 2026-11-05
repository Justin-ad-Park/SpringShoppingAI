package com.example.ailearning.l08advisors;

import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.OpenAiStubServer;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantTextMessage;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantToolCallMessage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import java.net.*;
import java.net.http.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class AdvisorLocalModelTest {
    private TestOutput output;

    @BeforeEach
    void 테스트_이름_준비(TestInfo info) {
        output = new TestOutput(info
                .getTestMethod()
                .orElseThrow()
                .getName());
    }

    static final OpenAiStubServer MODEL = new OpenAiStubServer();
    @Value("${local.server.port}") int port;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }
    @BeforeEach void reset() {
        MODEL
                .chat()
                .resetResponsesAndRequests();

        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("고정 응답"));
    }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void Advisor의_응답_처리는_요청의_반대_순서로_실행된다() throws Exception {
        try (var browser = browser()) {
            var response = post(browser, "/api/advisors/demo", "안녕");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(json(response).path("steps").toString()).isEqualTo("[\"A:before\",\"B:before\",\"B:after\",\"A:after\"]");
        }
    }
    @Test void 메모리_Advisor는_이전_대화를_중복_저장_없이_전달한다() throws Exception {
        try (var browser = browser()) {
            // Given: 첫 요청으로 메모리 Advisor에 이전 대화 저장
            post(browser, "/api/advisors/demo", "예산은 3만원");

            // When: 같은 세션에서 후속 질문 전달
            var response = post(browser, "/api/advisors/demo", "내 예산은?");

            // Then: 이전 대화가 한 번씩 전달·저장됐는지 검증
            assertThat(json(response).path("storedMessages").asInt()).isEqualTo(4);
            assertThat(MODEL.chat().getRequests().get(1).path("messages").toString()).contains("예산은 3만원");
            assertThat(MODEL.chat().getRequests().get(1).path("messages").size()).isEqualTo(4);
        }
    }
    @Test void 모델_호출에_실패해도_기본_Advisor에_사용자_질문이_남는다() throws Exception {
        try (var browser = browser()) {
            // Given: 가짜 모델이 HTTP 503을 반환하도록 설정
            MODEL
                    .chat()
                    .setHttpStatus(503);

            // When: 실패할 첫 질문 전달
            var failed = post(browser, "/api/advisors/demo", "실패한 요청의 예산은 3만원");

            // Then: 첫 요청의 실패 응답 확인
            assertThat(failed.statusCode()).isEqualTo(500);

            // Given: 다음 요청은 성공하도록 모델 상태 복구
            MODEL
                    .chat()
                    .setHttpStatus(200);

            // When: 같은 세션에서 후속 질문 전달
            var next = post(browser, "/api/advisors/demo", "이전 질문이 남아 있나요?");

            // Then: 실패한 요청의 사용자 질문도 메모리에 남았는지 검증
            assertThat(next.statusCode()).isEqualTo(200);
            var messages = MODEL
                    .chat()
                    .getRequests()
                    .get(1)
                    .path("messages");
            assertThat(messages.toString()).contains("실패한 요청의 예산은 3만원", "이전 질문이 남아 있나요?");
            // 실패한 user + 다음 user/assistant. 실패한 호출의 assistant는 없다.
            assertThat(json(next).path("storedMessages").asInt()).isEqualTo(3);
        }
    }
    @Test void 도구_루프_안쪽과_바깥쪽_Advisor의_실행_횟수가_다르다() throws Exception {
        // Given: 도구 호출 요청과 최종 답변을 순서대로 반환하도록 설정
        MODEL
                .chat()
                .setResponseSequence(createAssistantToolCallMessage("price", "getTeaPrice", "{}"), createAssistantTextMessage("허브티 12000원"));
        try (var browser = browser()) {
            // When: 도구가 등록된 Advisor API 호출
            var response = post(browser, "/api/advisors/tool?tools=true", "허브티 가격");

            // Then: Advisor 실행 순서·도구 결과 전달·메모리 저장 범위 검증
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(json(response).path("steps").toString()).isEqualTo( "[\"A:before\",\"B:before\",\"B:after\",\"B:before\",\"B:after\",\"A:after\"]");
            assertThat(MODEL.chat().getRequests()).hasSize(2);
            assertThat(MODEL.chat().getRequests().get(1).path("messages").toString()).contains("12000", "tool_call_id");
            // 기본 메모리 Advisor는 도구 루프 밖: user/최종 assistant만 저장한다.
            assertThat(json(response).path("storedMessages").asInt()).isEqualTo(2);
        }
    }
    @Test void 메모리_삭제는_다른_사용자_세션에_영향을_주지_않는다() throws Exception {
        try (var alice = browser(); var bob = browser()) {
            // Given: 서로 다른 세션에 이전 대화 저장
            post(alice, "/api/advisors/demo", "Alice의 질문");
            post(bob, "/api/advisors/demo", "Bob의 질문");
            assertThat(MODEL.chat().getRequests().get(1).path("messages").toString()).doesNotContain("Alice");

            // When: Alice 대화만 삭제
            output.printHttpResponse(alice.send(HttpRequest.newBuilder(uri("/api/advisors/demo"))
                                    .DELETE()
                                    .build(), HttpResponse.BodyHandlers.ofString()));

            // When: 삭제한 Alice 세션에서 새 질문 전달
            var aliceResponse = post(alice, "/api/advisors/demo", "새 질문");

            // Then: Alice는 새 대화만 저장됐는지 확인
            assertThat(json(aliceResponse).path("storedMessages").asInt()).isEqualTo(2);

            // When: Bob 세션에서 후속 질문 전달
            var bobResponse = post(bob, "/api/advisors/demo", "후속 질문");

            // Then: Bob의 이전 대화가 유지됐는지 검증
            assertThat(json(bobResponse).path("storedMessages").asInt()).isEqualTo(4);
        }
    }
    HttpClient browser() { return HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build(); }
    URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    HttpResponse<String> post(HttpClient http, String path, String text) throws Exception {
        return output.printHttpResponse(http.send(HttpRequest.newBuilder(uri(path))
                                .header("Content-Type", "text/plain; charset=UTF-8")
                                .POST(HttpRequest.BodyPublishers.ofString(text))
                                .build(), HttpResponse.BodyHandlers.ofString()));
    }
    HttpResponse<String> get(HttpClient http, String path) throws Exception {
        return output.printHttpResponse(http.send(HttpRequest.newBuilder(uri(path))
                                .GET()
                                .build(), HttpResponse.BodyHandlers.ofString()));
    }
    JsonNode json(HttpResponse<String> response) { return OpenAiStubServer.JSON.readTree(response.body()); }
}
