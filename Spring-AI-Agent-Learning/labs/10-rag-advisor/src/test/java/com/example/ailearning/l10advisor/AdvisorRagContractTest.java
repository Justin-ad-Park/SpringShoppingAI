package com.example.ailearning.l10advisor;

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
    static final ModelWireServer MODEL = start();
    @Value("${local.server.port}") int port;
    static ModelWireServer start() {
        try { return new ModelWireServer(); } catch (Exception error) { throw new IllegalStateException(error); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.embedding.options.model", () -> "learning-embedding");
        registry.add("spring.ai.openai.base-url", MODEL::url);
    }
    @BeforeEach void reset() { MODEL.requests.clear(); }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void readerSplitterStoreAndAdvisorPutRealDocumentInModelRequest() throws Exception {
        MODEL.responder = request -> ModelWireServer.message("미개봉 반품은 7일 이내입니다.");
        var response = ask("미개봉 반품 기간");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("7일");
        assertThat(MODEL.requests).hasSize(1);
        assertThat(MODEL.requests.getFirst().path("messages").toString()).contains("가상 쇼핑몰 반품 정책", "7일");
        assertThat(MODEL.embeddingRequests.toString()).contains("가상 쇼핑몰", "미개봉 반품 기간");
    }
    @Test void emptyRetrievalStillCallsModelUnlikeManualNoContextBranch() throws Exception {
        MODEL.responder = request -> ModelWireServer.message("관련 근거가 없습니다.");
        assertThat(ask("우주 여행").statusCode()).isEqualTo(200);
        assertThat(MODEL.requests).hasSize(1);
        assertThat(MODEL.requests.getFirst().path("messages").toString()).doesNotContain("수령 후 7일");
    }
    @Test void splitterCanSplitLongDocumentsWithoutCallingModel() {
        var chunks = TokenTextSplitter.builder().build().apply(List.of(new Document("A sealed product may be returned within seven days. ".repeat(500))));
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.getText()).isNotBlank());
        assertThat(MODEL.requests).isEmpty();
    }
    @Test void blankInputDoesNotCallModel() throws Exception {
        assertThat(ask(" ").statusCode()).isEqualTo(400);
        assertThat(MODEL.requests).isEmpty();
    }
    HttpResponse<String> ask(String question) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/advisor-rag"))
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(question)).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
