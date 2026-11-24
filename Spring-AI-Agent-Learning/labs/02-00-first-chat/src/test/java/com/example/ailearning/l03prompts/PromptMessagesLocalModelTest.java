package com.example.ailearning.l03prompts;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import com.example.ailearning.testsupport.output.TestOutput;
import com.example.ailearning.testsupport.http.LocalTestHttpClient;
import com.example.ailearning.testsupport.model.OpenAiStubServer;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
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

/** 03단원: system/user 전달과 이전 대화가 자동 포함되지 않는 요청을 확인한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class PromptMessagesLocalModelTest {
    private static final OpenAiStubServer MODEL_SERVER = new OpenAiStubServer();
    private final TestOutput output = new TestOutput();

    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> this.port, output);

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @DynamicPropertySource
    static void modelProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "tutor-test-model");
        registry.add("spring.ai.openai.base-url", MODEL_SERVER::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL_SERVER
                .chat()
                .resetResponsesAndRequests();
        MODEL_SERVER
                .chat()
                .setResponseText("로컬 테스트 모델의 고정 응답입니다.");
    }

    @AfterAll
    static void 로컬_모델_서버_종료() {
        MODEL_SERVER.close();
    }

    // 03단원: 서비스 소스를 바꾸지 않고 같은 system/user 구성을 로컬 모델 서버에 전달한다.
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

    // 03단원에서 읽기: 두 번째 요청에 현재 질문만 담기는지 확인합니다.
    // 실제 모델의 기억 능력을 평가하는 테스트는 아닙니다.
    @Test
    @DisplayName("로컬 MockModel의 다음 요청에 이전 대화를 자동 포함하지 않는다")
    void 다음_요청에_이전_대화를_자동으로_포함하지_않는다() throws Exception {
        // Given: 이름을 알리는 첫 요청 실행
        HttpResponse<String> firstResponse = ask("제 이름은 민수입니다.");
        assertThat(firstResponse.statusCode()).isEqualTo(200);

        // When: 이름을 묻는 다음 요청 실행
        HttpResponse<String> secondResponse = ask("제 이름이 무엇인가요?");

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

    private HttpResponse<String> ask(String message) throws Exception {
        return appClient.postText("/api/chat", message);
    }
}
