package com.example.ailearning;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import com.example.ailearning.testsupport.output.TestOutput;
import com.example.ailearning.testsupport.http.LocalTestHttpClient;
import com.example.ailearning.testsupport.model.OpenAiStubServer;
import static com.example.ailearning.testsupport.model.OpenAiChatStub.createAssistantTextMessage;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
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

    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> this.port, output);

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    private static final OpenAiStubServer MODEL_SERVER = new OpenAiStubServer();

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

    /**
     * 02장 보충: Spring MVC/Validation이 모델 호출 전에 잘못된 입력을 거부하는지 확인합니다.
     * https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/support/DefaultHandlerExceptionResolver.html
     * @throws Exception
     */
    @Test
    @DisplayName("잘못된 입력은 로컬 MockModel 호출 전에 거부한다")
    void 잘못된_입력은_로컬_모델_호출_전에_거부한다() throws Exception {
        HttpResponse<String> blankResponse = ask("   ");
        assertThat(blankResponse.statusCode()).isEqualTo(400);

        HttpResponse<String> oversizedResponse = ask("가".repeat(ChatController.MAX_MESSAGE_LENGTH + 1));
        assertThat(oversizedResponse.statusCode()).isEqualTo(400);
        assertThat(MODEL_SERVER.chat().getRequests()).isEmpty();
    }

    // curl 대신 애플리케이션의 /api/chat으로 HTTP 요청을 보내는 보조 메서드입니다.
    private HttpResponse<String> ask(String message) throws Exception {
        return appClient.postText("/api/chat", message);
    }

}
