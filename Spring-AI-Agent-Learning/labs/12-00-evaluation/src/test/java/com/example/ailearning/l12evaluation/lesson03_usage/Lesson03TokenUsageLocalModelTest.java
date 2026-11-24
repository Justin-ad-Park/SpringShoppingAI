package com.example.ailearning.l12evaluation.lesson03_usage;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import com.example.ailearning.testsupport.output.TestOutput;
import com.example.ailearning.testsupport.http.LocalTestHttpClient;
import com.example.ailearning.testsupport.model.OpenAiStubServer;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.config.import=", "spring.profiles.active=usage-only"})
class Lesson03TokenUsageLocalModelTest {
    private final TestOutput output = new TestOutput();

    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> this.port, output);

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    static final OpenAiStubServer MODEL = createModelServer();
    @Value("${local.server.port}") int port;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.model.embedding", () -> "none");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }
    @AfterAll
    static void stop() {
        MODEL.close();
    }

    @Test
    void 검색이나_임베딩_없이_한_응답의_답변과_사용량을_읽는다() throws Exception {
        MODEL
                .chat()
                .setResponseText("반갑습니다.");
        var response = appClient.postText("/api/evaluation/usage", "안녕");
        var body = OpenAiStubServer.JSON.readTree(response.body());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.path("answer").asString()).isEqualTo("반갑습니다.");
        assertThat(body.path("inputTokens").asInt()).isEqualTo(10);
        assertThat(body.path("outputTokens").asInt()).isEqualTo(5);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        assertThat(MODEL.embeddings().getRequests()).isEmpty();
    }

    private static OpenAiStubServer createModelServer() {
        var server = new OpenAiStubServer();
        server
                .chat()
                .setTokenUsage(10, 5);
        return server;
    }
}
