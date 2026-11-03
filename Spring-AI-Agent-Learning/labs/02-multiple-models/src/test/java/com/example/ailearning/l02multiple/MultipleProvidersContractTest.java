package com.example.ailearning.l02multiple;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.URI;
import java.net.http.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class MultipleProvidersContractTest {
    static final LocalProvidersServer PROVIDERS = new LocalProvidersServer();
    @Value("${local.server.port}") int port;

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

    @Test void openAiQualifierUsesOnlyOpenAiAdapterAndCredentials() throws Exception {
        var response = ask("openai");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("OpenAI 로컬 고정 응답");
        assertThat(PROVIDERS.openAiRequests).hasSize(1);
        assertThat(PROVIDERS.openAiRequests.getFirst().path("model").asString()).isEqualTo("openai-test-model");
        assertThat(PROVIDERS.openAiAuth).containsExactly("Bearer local-openai-key");
        assertThat(PROVIDERS.anthropicRequests).isEmpty();
    }
    @Test void anthropicQualifierUsesOnlyAnthropicAdapterAndCredentials() throws Exception {
        var response = ask("anthropic");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Anthropic 로컬 고정 응답");
        assertThat(PROVIDERS.anthropicRequests).hasSize(1);
        assertThat(PROVIDERS.anthropicRequests.getFirst().path("model").asString()).isEqualTo("anthropic-test-model");
        assertThat(PROVIDERS.anthropicAuth).containsExactly("local-anthropic-key");
        assertThat(PROVIDERS.openAiRequests).isEmpty();
    }
    HttpResponse<String> ask(String provider) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/multi-chat/"+provider))
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString("DI를 설명해줘")).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
