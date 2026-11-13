package com.example.ailearning.l09search;

import com.example.ailearning.testsupport.TestNamePrinter;
import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.LocalTestHttpClient;
import com.example.ailearning.testsupport.OpenAiStubServer;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class SearchLocalModelTest {
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
    void 문서와_질문을_임베딩해_관련_문서를_찾는다() throws Exception {
        var body = json(search("배송", ""));
        assertThat(body.path("vectorHits").get(0).path("id").asString()).isEqualTo("shipping");
        assertThat(MODEL.embeddings().getRequests().toString()).contains("미개봉", "배송", "learning-embedding");
    }

    @Test
    void 표현이_달라도_벡터_검색으로_관련_문서를_찾을_수_있다() throws Exception {
        var body = json(search("운송 요금", ""));
        assertThat(body.path("keywordHits").isEmpty()).isTrue();
        assertThat(body.path("vectorHits").get(0).path("id").asString()).isEqualTo("shipping");
    }

    @Test
    void 검색에_카테고리와_개수와_유사도_조건을_적용한다() throws Exception {
        var body = json(search("반품", "&category=returns&topK=1&threshold=0.8"));
        assertThat(body.path("vectorHits").size()).isEqualTo(1);
        assertThat(body.path("vectorHits").get(0).path("category").asString()).isEqualTo("returns");
        assertThat(json(search("반품", "&category=shipping&threshold=0.8")).path("vectorHits").isEmpty()).isTrue();
        assertThat(json(search("우주", "&threshold=0.8")).path("vectorHits").isEmpty()).isTrue();
    }

    @Test
    void 텍스트를_임베딩한_벡터를_반환한다() throws Exception {
        var response = appClient.postText("/api/embedding", "배송");
        assertThat(json(response).path("dimensions").asInt()).isEqualTo(3);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @ValueSource(strings = {"&topK=0", "&threshold=2", "&category=unknown"})
    void 잘못된_검색_조건은_거부한다(String options) throws Exception {
        assertThat(search("반품", options).statusCode()).isEqualTo(400);
    }

    HttpResponse<String> search(String query, String options) throws Exception {
        return appClient.get("/api/search?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + options);
    }

    tools.jackson.databind.JsonNode json(HttpResponse<String> response) { return OpenAiStubServer.JSON.readTree(response.body()); }

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
