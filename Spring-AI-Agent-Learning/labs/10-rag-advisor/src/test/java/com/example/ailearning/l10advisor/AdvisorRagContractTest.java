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

    @Test void Reader와_Splitter와_Advisor로_문서를_모델에_전달한다() throws Exception {
        MODEL.responder = request -> ModelWireServer.message("미개봉 반품은 7일 이내입니다.");
        var response = ask("미개봉 반품 기간");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("7일");
        assertThat(MODEL.requests).hasSize(1);
        assertThat(MODEL.requests.getFirst().path("messages").toString()).contains("가상 쇼핑몰 반품 정책", "7일");
        assertThat(MODEL.embeddingRequests.toString()).contains("가상 쇼핑몰", "미개봉 반품 기간");
    }
    @Test void 기본_RAG_Advisor는_검색_결과가_없어도_모델을_호출한다() throws Exception {
        MODEL.responder = request -> ModelWireServer.message("관련 근거가 없습니다.");
        assertThat(ask("우주 여행").statusCode()).isEqualTo(200);
        assertThat(MODEL.requests).hasSize(1);
        assertThat(MODEL.requests.getFirst().path("messages").toString()).doesNotContain("수령 후 7일");
    }
    @Test void 긴_문서는_모델_호출_없이_여러_조각으로_나눈다() {
        var chunks = TokenTextSplitter.builder().build().apply(List.of(new Document("A sealed product may be returned within seven days. ".repeat(500))));
        결과출력("문서 조각 수", chunks.size());
        결과출력("첫 조각 원문", chunks.getFirst().getText());
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.getText()).isNotBlank());
        assertThat(MODEL.requests).isEmpty();
    }
    @Test void 공백_질문은_모델을_호출하지_않는다() throws Exception {
        assertThat(ask(" ").statusCode()).isEqualTo(400);
        assertThat(MODEL.requests).isEmpty();
    }
    HttpResponse<String> ask(String question) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return 응답출력(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/advisor-rag"))
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(question)).build(), HttpResponse.BodyHandlers.ofString()));
        }
    }
}
