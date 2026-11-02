package com.example.ailearning.l11judge;

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
class EvaluationApiContractTest {
    static final LocalModelServer MODEL = new LocalModelServer();
    @Value("${local.server.port}") int port;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::url);
    }
    @BeforeEach void reset() { MODEL.requests.clear(); MODEL.answer = "YES"; }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void relevanceSendsQuestionContextAndCandidateToJudge() throws Exception {
        var response = post("relevance", "미개봉 반품 기간", "미개봉은 7일 이내 반품", "반품은 7일 이내입니다.");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("true");
        assertThat(MODEL.requests).hasSize(1);
        assertThat(MODEL.requests.getFirst().path("messages").toString()).contains("미개봉 반품 기간", "미개봉은 7일 이내 반품", "반품은 7일 이내입니다.");
    }
    @Test void factCheckingSendsDocumentAndClaimAndMapsNoToFalse() throws Exception {
        MODEL.answer = "NO";
        var response = post("facts", "미개봉 반품 기간", "미개봉은 7일 이내 반품", "30일 이내 반품입니다.");
        assertThat(response.body()).isEqualTo("false");
        assertThat(MODEL.requests).hasSize(1);
        assertThat(MODEL.requests.getFirst().path("messages").toString()).contains("Document:", "Claim:", "7일", "30일");
    }
    @Test void unexpectedJudgeTextIsNotAPass() throws Exception {
        MODEL.answer = "maybe";
        assertThat(post("relevance", "질문", "근거", "답변").body()).isEqualTo("false");
    }
    @Test void blankCandidateIsRejectedBeforeJudgeCall() throws Exception {
        assertThat(post("relevance", "질문", "근거", " ").statusCode()).isEqualTo(400);
        assertThat(MODEL.requests).isEmpty();
    }
    HttpResponse<String> post(String kind, String question, String context, String answer) throws Exception {
        var body = LocalModelServer.JSON.createObjectNode().put("question", question).put("context", context).put("answer", answer);
        try (var http = HttpClient.newHttpClient()) {
            return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/evaluate/"+kind))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }
}
