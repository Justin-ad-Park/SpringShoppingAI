package com.example.ailearning.l05tools;

import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.OpenAiStubServer;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantTextMessage;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantToolCallMessage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.URI;
import java.net.http.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class ToolCallingLocalModelTest {
    private TestOutput output;

    @BeforeEach
    void 테스트_이름_준비(TestInfo info) {
        output = new TestOutput(info
                .getTestMethod()
                .orElseThrow()
                .getName());
    }

    static final OpenAiStubServer MODEL = new OpenAiStubServer();
    @Value("${local.server.port}") int port;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }
    @BeforeEach void reset() { MODEL
            .chat()
            .resetResponsesAndRequests(); }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void Java로_직접_상품을_검색하면_모델을_호출하지_않는다() throws Exception {
        var response = get("/direct?category=%EC%9D%8C%EB%A3%8C&maxPrice=15000");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("P01").doesNotContain("P02");
        assertThat(MODEL.chat().getRequests()).isEmpty();
    }
    @Test void 모델이_요청한_도구를_실행하고_결과를_다시_전달한다() throws Exception {
        MODEL
                .chat()
                .setResponseSequence(
                createAssistantToolCallMessage("search-1", "searchProducts", "{\"category\":\"음료\",\"maxPrice\":15000}"),
                createAssistantTextMessage("허브티 12000원입니다."));
        var response = post("15000원 이하 음료");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(MODEL.chat().getRequests()).hasSize(2);
        assertThat(MODEL.chat().getRequests().getFirst().path("tools").toString()).contains("searchProducts", "calculateShipping");
        String messages = MODEL
                .chat()
                .getRequests()
                .get(1)
                .path("messages")
                .toString();
        assertThat(messages).contains("tool_call_id", "search-1", "P01").doesNotContain("P02");
    }

    @Test void 배송비는_상품_목록의_실제_가격으로_계산한다() throws Exception {
        MODEL
                .chat()
                .setResponseSequence(
                createAssistantToolCallMessage("shipping-1", "calculateShipping", "{\"productId\":\"P02\"}"),
                createAssistantTextMessage("총 31000원입니다."));
        assertThat(post("커피 배송비 포함 가격").statusCode()).isEqualTo(200);
        assertThat(MODEL.chat().getRequests().get(1).path("messages").toString()).contains("31000", "3000");
    }

    @Test void 인사는_도구를_호출하지_않고_답할_수_있다() throws Exception {
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("안녕하세요."));
        assertThat(post("안녕").body()).isEqualTo("안녕하세요.");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    @Test void 공백_질문은_모델을_호출하지_않는다() throws Exception {
        assertThat(post(" ").statusCode()).isEqualTo(400);
        assertThat(MODEL.chat().getRequests()).isEmpty();
    }

    HttpResponse<String> get(String path) throws Exception {
        return send(HttpRequest.newBuilder(uri(path))
                        .GET()
                        .build());
    }

    HttpResponse<String> post(String question) throws Exception {
        return send(HttpRequest.newBuilder(uri("/chat"))
                        .header("Content-Type", "text/plain; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(question))
                        .build());
    }

    URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + "/api/tools" + path);
    }

    HttpResponse<String> send(HttpRequest request) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return output.printHttpResponse(http.send(request, HttpResponse.BodyHandlers.ofString()));
        }
    }
}
