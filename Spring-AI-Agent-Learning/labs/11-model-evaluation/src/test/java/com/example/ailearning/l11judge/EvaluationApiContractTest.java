package com.example.ailearning.l11judge;

import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.OpenAiStubServer;
import static com.example.ailearning.testsupport.OpenAiStubServer.text;
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
    private TestOutput output;

    @BeforeEach
    void 테스트_이름_준비(TestInfo info) {
        output = new TestOutput(info.getTestMethod().orElseThrow().getName());
    }

    static final OpenAiStubServer MODEL = new OpenAiStubServer().usage(10, 5);
    @Value("${local.server.port}") int port;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::baseUrl);
    }
    @BeforeEach void reset() { MODEL.resetChat(); MODEL.respondWith(text("YES")); }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void 질문과_근거와_후보_답변을_평가_모델에_전달한다() throws Exception {
        var response = post("relevance", "미개봉 반품 기간", "미개봉은 7일 이내 반품", "반품은 7일 이내입니다.");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("true");
        assertThat(MODEL.chatRequests()).hasSize(1);
        assertThat(MODEL.chatRequests().getFirst().path("messages").toString()).contains("미개봉 반품 기간", "미개봉은 7일 이내 반품", "반품은 7일 이내입니다.");
    }
    @Test void 근거와_주장을_평가하고_NO를_false로_변환한다() throws Exception {
        MODEL.respondWith(text("NO"));
        var response = post("facts", "미개봉 반품 기간", "미개봉은 7일 이내 반품", "30일 이내 반품입니다.");
        assertThat(response.body()).isEqualTo("false");
        assertThat(MODEL.chatRequests()).hasSize(1);
        assertThat(MODEL.chatRequests().getFirst().path("messages").toString()).contains("Document:", "Claim:", "7일", "30일");
    }
    @Test void 예상하지_못한_평가_응답은_통과로_처리하지_않는다() throws Exception {
        MODEL.respondWith(text("maybe"));
        assertThat(post("relevance", "질문", "근거", "답변").body()).isEqualTo("false");
    }
    @Test void 빈_후보_답변은_평가_모델_호출_전에_거부한다() throws Exception {
        assertThat(post("relevance", "질문", "근거", " ").statusCode()).isEqualTo(400);
        assertThat(MODEL.chatRequests()).isEmpty();
    }
    HttpResponse<String> post(String kind, String question, String context, String answer) throws Exception {
        var body = OpenAiStubServer.JSON.createObjectNode().put("question", question).put("context", context).put("answer", answer);
        try (var http = HttpClient.newHttpClient()) {
            return output.response(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/evaluate/"+kind))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),
                    HttpResponse.BodyHandlers.ofString()));
        }
    }
}
