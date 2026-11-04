package com.example.ailearning.l02multiple;

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
    private String 테스트이름;

    @BeforeEach
    void 테스트_이름_준비(TestInfo info) {
        테스트이름 = info.getTestMethod().orElseThrow().getName();
    }

    private void 결과출력(String 항목, Object 결과) {
        System.out.printf("[%s] %s: %s%n", 테스트이름, 항목, 결과);
    }

    private HttpResponse<String> 응답출력(HttpResponse<String> response) {
        결과출력("요청", response.request().method() + " " + response.uri().getPath());
        결과출력("HTTP " + response.statusCode(), response.body());
        return response;
    }

    static final LocalProvidersServer PROVIDERS = new LocalProvidersServer();
    @Value("${local.server.port}") int port;
    @Autowired SameProviderModelsService sameProviderModels;

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-openai-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "openai-test-model");
        registry.add("spring.ai.openai.base-url", () -> PROVIDERS.baseUrl()+"/v1");
        registry.add("spring.ai.anthropic.api-key", () -> "local-anthropic-key");
        registry.add("spring.ai.anthropic.chat.model", () -> "anthropic-test-model");
        registry.add("spring.ai.anthropic.base-url", PROVIDERS::baseUrl);
    }
    @BeforeEach void reset() {
        PROVIDERS.openAiRequests.clear(); PROVIDERS.anthropicRequests.clear();
        PROVIDERS.openAiAuth.clear(); PROVIDERS.anthropicAuth.clear();
    }
    @AfterAll static void stop() { PROVIDERS.close(); }

    @Test void OpenAI로_지정한_클라이언트는_OpenAI만_호출한다() throws Exception {
        var response = ask("openai");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("OpenAI 로컬 고정 응답");
        assertThat(PROVIDERS.openAiRequests).hasSize(1);
        assertThat(PROVIDERS.openAiRequests.getFirst().path("model").asString()).isEqualTo("openai-test-model");
        assertThat(PROVIDERS.openAiAuth).containsExactly("Bearer local-openai-key");
        assertThat(PROVIDERS.anthropicRequests).isEmpty();
    }
    @Test void Anthropic으로_지정한_클라이언트는_Anthropic만_호출한다() throws Exception {
        var response = ask("anthropic");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Anthropic 로컬 고정 응답");
        assertThat(PROVIDERS.anthropicRequests).hasSize(1);
        assertThat(PROVIDERS.anthropicRequests.getFirst().path("model").asString()).isEqualTo("anthropic-test-model");
        assertThat(PROVIDERS.anthropicAuth).containsExactly("local-anthropic-key");
        assertThat(PROVIDERS.openAiRequests).isEmpty();
    }
    @Test void 요청별_모델을_선택해도_기본_모델은_유지된다() throws Exception {
        결과출력("모델 A 응답", sameProviderModels.ask("짧게 설명해줘", "openai-model-a"));
        결과출력("모델 B 응답", sameProviderModels.ask("자세히 설명해줘", "openai-model-b"));
        ask("openai");

        결과출력("요청별 모델", PROVIDERS.openAiRequests.stream().map(request -> request.path("model").asString()).toList());
        assertThat(PROVIDERS.openAiRequests)
                .extracting(request -> request.path("model").asString())
                .containsExactly("openai-model-a", "openai-model-b", "openai-test-model");
        assertThat(PROVIDERS.openAiAuth)
                .containsExactly("Bearer local-openai-key", "Bearer local-openai-key", "Bearer local-openai-key");
        assertThat(PROVIDERS.anthropicRequests).isEmpty();
    }

    HttpResponse<String> ask(String provider) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return 응답출력(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/multi-chat/"+provider))
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString("DI를 설명해줘")).build(), HttpResponse.BodyHandlers.ofString()));
        }
    }
}
