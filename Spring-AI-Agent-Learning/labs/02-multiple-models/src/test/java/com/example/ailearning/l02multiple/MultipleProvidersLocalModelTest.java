package com.example.ailearning.l02multiple;

import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.OpenAiStubServer;
import com.example.ailearning.testsupport.AnthropicStubServer;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantTextMessage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.URI;
import java.net.http.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class MultipleProvidersLocalModelTest {
    private TestOutput output;

    @BeforeEach
    void 테스트_이름_준비(TestInfo info) {
        output = new TestOutput(info
                .getTestMethod()
                .orElseThrow()
                .getName());
    }

    static final OpenAiStubServer OPENAI = new OpenAiStubServer();
    static final AnthropicStubServer ANTHROPIC = new AnthropicStubServer();
    @Value("${local.server.port}") int port;
    @Autowired SameProviderModelsService sameProviderModels;

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-openai-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "openai-test-model");
        registry.add("spring.ai.openai.base-url", OPENAI::getBaseUrl);
        registry.add("spring.ai.anthropic.api-key", () -> "local-anthropic-key");
        registry.add("spring.ai.anthropic.chat.model", () -> "anthropic-test-model");
        registry.add("spring.ai.anthropic.base-url", ANTHROPIC::getBaseUrl);
    }
    @BeforeEach void reset() {
        OPENAI
                .chat()
                .resetResponsesAndRequests();
        OPENAI
                .chat()
                .setTokenUsage(10, 5);
        OPENAI
                .chat()
                .setResponseMessage(createAssistantTextMessage("OpenAI 로컬 고정 응답"));
        ANTHROPIC.resetMessageResponseAndRequests();
        ANTHROPIC.setMessageResponseText("Anthropic 로컬 고정 응답");
    }
    @AfterAll static void stop() { OPENAI.close(); ANTHROPIC.close(); }

    @Test void OpenAI로_지정한_클라이언트는_OpenAI만_호출한다() throws Exception {
        var response = ask("openai");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("OpenAI 로컬 고정 응답");
        assertThat(OPENAI.chat().getRequests()).hasSize(1);
        assertThat(OPENAI.chat().getRequests().getFirst().path("model").asString()).isEqualTo("openai-test-model");
        assertThat(OPENAI.chat().getAuthorizationHeaders()).containsExactly("Bearer local-openai-key");
        assertThat(ANTHROPIC.getMessageRequests()).isEmpty();
    }
    @Test void Anthropic으로_지정한_클라이언트는_Anthropic만_호출한다() throws Exception {
        var response = ask("anthropic");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Anthropic 로컬 고정 응답");
        assertThat(ANTHROPIC.getMessageRequests()).hasSize(1);
        assertThat(ANTHROPIC.getMessageRequests().getFirst().path("model").asString()).isEqualTo("anthropic-test-model");
        assertThat(ANTHROPIC.getMessageApiKeyHeaders()).containsExactly("local-anthropic-key");
        assertThat(OPENAI.chat().getRequests()).isEmpty();
    }
    @Test void 요청별_모델을_선택해도_기본_모델은_유지된다() throws Exception {
        // Given: 같은 제공 업체에 서로 다른 모델을 지정한 두 요청 실행
        output.printValue("모델 A 응답", sameProviderModels.ask("짧게 설명해줘", "openai-model-a"));
        output.printValue("모델 B 응답", sameProviderModels.ask("자세히 설명해줘", "openai-model-b"));

        // When: 모델을 따로 지정하지 않은 기본 클라이언트 호출
        ask("openai");

        // Then: 각 요청의 모델과 기본 모델 설정 유지 여부 검증
        output.printValue("요청별 모델", OPENAI
                        .chat()
                        .getRequests()
                        .stream()
                        .map(request -> request
                                .path("model")
                                .asString())
                        .toList());
        assertThat(OPENAI
                        .chat()
                        .getRequests())
                .extracting(request -> request
                        .path("model")
                        .asString())
                .containsExactly("openai-model-a", "openai-model-b", "openai-test-model");
        assertThat(OPENAI.chat().getAuthorizationHeaders()).containsExactly("Bearer local-openai-key", "Bearer local-openai-key", "Bearer local-openai-key");
        assertThat(ANTHROPIC.getMessageRequests()).isEmpty();
    }

    HttpResponse<String> ask(String provider) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return output.printHttpResponse(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/multi-chat/"+provider))
                                    .header("Content-Type", "text/plain; charset=UTF-8")
                                    .POST(HttpRequest.BodyPublishers.ofString("DI를 설명해줘"))
                                    .build(), HttpResponse.BodyHandlers.ofString()));
        }
    }
}
