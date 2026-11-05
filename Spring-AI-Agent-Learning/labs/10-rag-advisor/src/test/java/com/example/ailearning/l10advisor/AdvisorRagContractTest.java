package com.example.ailearning.l10advisor;

import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.OpenAiStubServer;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantTextMessage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import java.net.URI;
import java.net.http.*;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class AdvisorRagContractTest {
    private TestOutput output;

    @BeforeEach
    void 테스트_이름_준비(TestInfo info) {
        output = new TestOutput(info
                .getTestMethod()
                .orElseThrow()
                .getName());
    }

    static final OpenAiStubServer MODEL = createModelServer();
    @Value("${local.server.port}") int port;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.embedding.options.model", () -> "learning-embedding");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }
    @BeforeEach void reset() { MODEL
            .chat()
            .resetResponsesAndRequests(); }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void Reader와_Splitter와_Advisor로_문서를_모델에_전달한다() throws Exception {
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("미개봉 반품은 7일 이내입니다."));
        var response = ask("미개봉 반품 기간");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("7일");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        assertThat(MODEL.chat().getRequests().getFirst().path("messages").toString()).contains("가상 쇼핑몰 반품 정책", "7일");
        assertThat(MODEL.embeddings().getRequests().toString()).contains("가상 쇼핑몰", "미개봉 반품 기간");
    }
    @Test void 기본_RAG_Advisor는_검색_결과가_없어도_모델을_호출한다() throws Exception {
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("관련 근거가 없습니다."));
        assertThat(ask("우주 여행").statusCode()).isEqualTo(200);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        assertThat(MODEL.chat().getRequests().getFirst().path("messages").toString()).doesNotContain("수령 후 7일");
    }
    @Test void 긴_문서는_모델_호출_없이_여러_조각으로_나눈다() {
        var chunks = TokenTextSplitter.builder()
                .build()
                .apply(List.of(new Document("A sealed product may be returned within seven days. ".repeat(500))));
        output.printValue("문서 조각 수", chunks.size());
        output.printValue("첫 조각 원문", chunks
                        .getFirst()
                        .getText());
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks)
                .allSatisfy(chunk -> assertThat(chunk.getText())
                        .isNotBlank());
        assertThat(MODEL.chat().getRequests()).isEmpty();
    }
    @Test void 공백_질문은_모델을_호출하지_않는다() throws Exception {
        assertThat(ask(" ").statusCode()).isEqualTo(400);
        assertThat(MODEL.chat().getRequests()).isEmpty();
    }
    HttpResponse<String> ask(String question) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return output.printHttpResponse(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/advisor-rag"))
                                    .header("Content-Type", "text/plain; charset=UTF-8")
                                    .POST(HttpRequest.BodyPublishers.ofString(question))
                                    .build(), HttpResponse.BodyHandlers.ofString()));
        }
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
        server
                .chat()
                .setTokenUsage(10, 5);
        return server;
    }
}
