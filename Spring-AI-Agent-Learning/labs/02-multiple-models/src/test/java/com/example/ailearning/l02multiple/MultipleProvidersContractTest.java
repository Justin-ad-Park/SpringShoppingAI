package com.example.ailearning.l02multiple;

import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.OpenAiStubServer;
import com.example.ailearning.testsupport.AnthropicStubServer;
import static com.example.ailearning.testsupport.OpenAiStubServer.text;
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
class MultipleProvidersContractTest {
    private TestOutput output;

    @BeforeEach
    void 테스트_이름_준비(TestInfo info) {
        output = new TestOutput(info.getTestMethod().orElseThrow().getName());
    }

    static final OpenAiStubServer OPENAI = new OpenAiStubServer().usage(10, 5);
    static final AnthropicStubServer ANTHROPIC = new AnthropicStubServer();
    @Value("${local.server.port}") int port;
    @Autowired SameProviderModelsService sameProviderModels;

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-openai-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "openai-test-model");
        registry.add("spring.ai.openai.base-url", OPENAI::baseUrl);
        registry.add("spring.ai.anthropic.api-key", () -> "local-anthropic-key");
        registry.add("spring.ai.anthropic.chat.model", () -> "anthropic-test-model");
        registry.add("spring.ai.anthropic.base-url", ANTHROPIC::baseUrl);
    }
    @BeforeEach void reset() {
        OPENAI.resetChat();
        OPENAI.respondWith(text("OpenAI 로컬 고정 응답"));
        ANTHROPIC.reset();
        ANTHROPIC.respondText("Anthropic 로컬 고정 응답");
    }
    @AfterAll static void stop() { OPENAI.close(); ANTHROPIC.close(); }

    @Test void OpenAI로_지정한_클라이언트는_OpenAI만_호출한다() throws Exception {
        var response = ask("openai");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("OpenAI 로컬 고정 응답");
        assertThat(OPENAI.chatRequests()).hasSize(1);
        assertThat(OPENAI.chatRequests().getFirst().path("model").asString()).isEqualTo("openai-test-model");
        assertThat(OPENAI.authorizationHeaders()).containsExactly("Bearer local-openai-key");
        assertThat(ANTHROPIC.requests()).isEmpty();
    }
    @Test void Anthropic으로_지정한_클라이언트는_Anthropic만_호출한다() throws Exception {
        var response = ask("anthropic");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Anthropic 로컬 고정 응답");
        assertThat(ANTHROPIC.requests()).hasSize(1);
        assertThat(ANTHROPIC.requests().getFirst().path("model").asString()).isEqualTo("anthropic-test-model");
        assertThat(ANTHROPIC.apiKeyHeaders()).containsExactly("local-anthropic-key");
        assertThat(OPENAI.chatRequests()).isEmpty();
    }
    @Test void 요청별_모델을_선택해도_기본_모델은_유지된다() throws Exception {
        output.value("모델 A 응답", sameProviderModels.ask("짧게 설명해줘", "openai-model-a"));
        output.value("모델 B 응답", sameProviderModels.ask("자세히 설명해줘", "openai-model-b"));
        ask("openai");

        output.value("요청별 모델", OPENAI.chatRequests().stream().map(request -> request.path("model").asString()).toList());
        assertThat(OPENAI.chatRequests())
                .extracting(request -> request.path("model").asString())
                .containsExactly("openai-model-a", "openai-model-b", "openai-test-model");
        assertThat(OPENAI.authorizationHeaders())
                .containsExactly("Bearer local-openai-key", "Bearer local-openai-key", "Bearer local-openai-key");
        assertThat(ANTHROPIC.requests()).isEmpty();
    }

    HttpResponse<String> ask(String provider) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return output.response(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/multi-chat/"+provider))
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString("DI를 설명해줘")).build(), HttpResponse.BodyHandlers.ofString()));
        }
    }
}
