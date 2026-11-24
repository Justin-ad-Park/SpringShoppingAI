package com.example.ailearning.l09search.lesson02_store;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.config.import=", "spring.profiles.active=basic-search"})
class Lesson02BasicVectorSearchLocalModelTest {
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
        registry.add("spring.ai.openai.embedding.options.model", () -> "learning-embedding");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }
    @AfterAll
    static void stop() {
        MODEL.close();
    }

    @Test
    void 문서를_적재하고_query와_topK로_검색한다() throws Exception {
        var response = appClient.get("/api/search/basic?query=" +
                java.net.URLEncoder.encode("배송", java.nio.charset.StandardCharsets.UTF_8) + "&topK=1");
        var documents = OpenAiStubServer.JSON.readTree(response.body());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(documents.size()).isEqualTo(1);
        assertThat(documents.get(0).path("id").asString()).isEqualTo("shipping");
    }

    private static OpenAiStubServer createModelServer() {
        var server = new OpenAiStubServer();
        server
                .embeddings()
                .setVectorFunction(text -> {
                    if (text.contains("배송") || text.contains("운송")) return new float[]{1, 0, 0};
                    if (text.contains("반품") || text.contains("개봉")) return new float[]{0, 1, 0};
                    return new float[]{0, 0, 1};
                });
        return server;
    }
}
