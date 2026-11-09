package com.example.ailearning;

import com.example.ailearning.testsupport.TestNamePrinter;
import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.OpenAiStubServer;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantTextMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.DisplayName;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 Spring AI 어댑터를 사용하되 외부 모델 API만 로컬 HTTP 서버로 대체합니다.
// 02장에서는 modelProperties와 정상 호출 테스트만 읽고, 서버 구현은 공통 test-support 모듈이 제공합니다.
// RANDOM_PORT는 테스트할 애플리케이션 서버에 사용 가능한 포트를 배정합니다.
@DisplayName("첫 Chat 호출 — 로컬 MockModel 계약 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class FirstChatLocalModelTest {
    private final TestOutput output = new TestOutput();

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    private static final OpenAiStubServer MODEL_SERVER = new OpenAiStubServer();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    // 테스트에서만 적용하며 application.yml의 같은 설정을 덮어씁니다.
    @DynamicPropertySource
    static void modelProperties(DynamicPropertyRegistry registry) {
        // 실제 키 대신 더미 키를 사용합니다. 아래 인증 헤더 검증에도 쓰이므로 유지합니다.
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        // 실제 모델 대신 요청에 담길 검증용 모델 이름입니다.
        registry.add("spring.ai.openai.chat.options.model", () -> "tutor-test-model");
        // 외부 제공 업체 대신 로컬 가짜 모델 서버로 요청을 보냅니다.
        registry.add("spring.ai.openai.base-url",
                MODEL_SERVER::getBaseUrl);
    }

    // 각 테스트 전에 기록을 비워 다른 테스트의 요청이 섞이지 않게 합니다.
    @BeforeEach
    void resetRequests() {
        MODEL_SERVER
                .chat()
                .resetResponsesAndRequests();
        MODEL_SERVER
                .chat()
                .setTokenUsage(10, 10);
        MODEL_SERVER
                .chat()
                .setResponseMessage(createAssistantTextMessage("로컬 테스트 모델의 고정 응답입니다."));

    }

    // 모든 테스트가 끝나면 가짜 모델 서버를 종료합니다.
    @AfterAll
    static void stopModelServer() {
        MODEL_SERVER.close();
    }

    // 02장 핵심: 질문 전달과 고정 응답 반환을 확인합니다.
    @Test
    @DisplayName("로컬 MockModel에 질문을 전달하고 고정 응답을 반환한다")
    void 로컬_모델에_질문을_보내고_고정_응답을_받는다() throws Exception {
        HttpResponse<String> response = ask("스프링이 무엇인가요?");
        output.printHttpResponse("LocalMockModel / 정상 호출 / 정상 질문", response);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("로컬 테스트 모델의 고정 응답입니다.");
        assertThat(MODEL_SERVER.chat().getRequests()).hasSize(1);
        JsonNode request = MODEL_SERVER
                .chat()
                .getRequests()
                .getFirst();
        assertThat(request.path("model").asText()).isEqualTo("tutor-test-model");
        assertThat(request.path("messages").size()).isEqualTo(1);
        assertThat(request.at("/messages/0/role").asText()).isEqualTo("user");
        assertThat(request.at("/messages/0/content").asText()).isEqualTo("스프링이 무엇인가요?");
        // 선택 심화: 더미 키가 제공 업체 요청의 인증 헤더에 들어가는지도 확인합니다.
        assertThat(MODEL_SERVER.chat().getAuthorizationHeaders()).containsExactly("Bearer local-test-key");
    }

    // 03장: 서비스 소스를 바꾸지 않고 같은 system/user 구성을 로컬 모델 서버에 전달한다.
    @Test
    void system과_user_메시지를_한_요청에_함께_전달한다() {
        // Given: 고정 응답 서버와 두 메시지에 담을 내용
        String instruction = "Spring 입문자를 가르치는 튜터입니다. 쉬운 한국어로 세 문장 이내로 설명하세요.";
        String question = "DI가 뭐야?";

        // When: 실제 서비스와 같은 요청 구성에 system 메시지를 추가한다.
        String answer = chatClientBuilder
                .build()
                .prompt()
                .system(instruction)
                .user(question)
                .call()
                .content();
        output.printValue("고정 응답", answer);

        // Then: 두 메시지가 한 HTTP 요청에 담겼는지 확인한다. 모델의 지시 준수는 검사하지 않는다.
        assertThat(answer).isEqualTo("로컬 테스트 모델의 고정 응답입니다.");
        assertThat(MODEL_SERVER.chat().getRequests()).hasSize(1);
        JsonNode messages = MODEL_SERVER.chat().getRequests().getFirst().path("messages");
        assertThat(messages.size()).isEqualTo(2);
        assertThat(messages.get(0).path("role").asText()).isEqualTo("system");
        assertThat(messages.get(0).path("content").asText()).isEqualTo(instruction);
        assertThat(messages.get(1).path("role").asText()).isEqualTo("user");
        assertThat(messages.get(1).path("content").asText()).isEqualTo(question);
    }

    /**
     * 02장 보충: Spring MVC/Validation이 모델 호출 전에 잘못된 입력을 거부하는지 확인합니다.
     * https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/support/DefaultHandlerExceptionResolver.html
     * @throws Exception
     */
    @Test
    @DisplayName("잘못된 입력은 로컬 MockModel 호출 전에 거부한다")
    void 잘못된_입력은_로컬_모델_호출_전에_거부한다() throws Exception {
        HttpResponse<String> blankResponse = ask("   ");
        output.printHttpResponse("LocalMockModel / 입력 검증 / 공백 입력", blankResponse);
        assertThat(blankResponse.statusCode()).isEqualTo(400);

        HttpResponse<String> oversizedResponse = ask("가".repeat(ChatController.MAX_MESSAGE_LENGTH + 1));
        output.printHttpResponse("LocalMockModel / 입력 검증 / 최대 길이보다 한 글자 긴 입력", oversizedResponse);
        assertThat(oversizedResponse.statusCode()).isEqualTo(400);
        assertThat(MODEL_SERVER.chat().getRequests()).isEmpty();
    }

    // 03장에서 읽기: 두 번째 요청에 현재 질문만 담기는지 확인합니다.
    // 실제 모델의 기억 능력을 평가하는 테스트는 아닙니다.
    @Test
    @DisplayName("로컬 MockModel의 다음 요청에 이전 대화를 자동 포함하지 않는다")
    void 다음_요청에_이전_대화를_자동으로_포함하지_않는다() throws Exception {
        // Given: 이름을 알리는 첫 요청 실행
        HttpResponse<String> firstResponse = ask("제 이름은 민수입니다.");
        output.printHttpResponse("LocalMockModel / 대화 전달 / 첫 번째 요청", firstResponse);
        assertThat(firstResponse.statusCode()).isEqualTo(200);

        // When: 이름을 묻는 다음 요청 실행
        HttpResponse<String> secondResponse = ask("제 이름이 무엇인가요?");
        output.printHttpResponse("LocalMockModel / 대화 전달 / 두 번째 요청", secondResponse);

        // Then: 후속 응답과 현재 질문만 담긴 두 번째 모델 요청 검증
        assertThat(secondResponse.statusCode()).isEqualTo(200);
        assertThat(MODEL_SERVER.chat().getRequests()).hasSize(2);
        JsonNode second = MODEL_SERVER
                .chat()
                .getRequests()
                .get(1);
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

}
