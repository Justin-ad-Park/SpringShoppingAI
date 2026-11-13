package com.example.ailearning.l12judge;

import com.example.ailearning.testsupport.TestNamePrinter;
import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.LocalTestHttpClient;
import com.example.ailearning.testsupport.OpenAiStubServer;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantTextMessage;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class EvaluationApiLocalModelTest {
    private final TestOutput output = new TestOutput();

    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> this.port, output);

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    static final OpenAiStubServer MODEL = new OpenAiStubServer();
    @Value("${local.server.port}")
    int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void reset() {
        MODEL
                .chat()
                .resetResponsesAndRequests();
        MODEL
                .chat()
                .setTokenUsage(10, 5);
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("YES"));
    }

    @AfterAll
    static void stop() {
        MODEL.close();
    }

    @Test
    void 질문과_근거와_후보_답변을_평가_모델에_전달한다() throws Exception {
        var response = post("relevance", "미개봉 반품 기간", "미개봉은 7일 이내 반품", "반품은 7일 이내입니다.");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("true");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        assertThat(MODEL.chat().getRequests().getFirst().path("messages").toString()).contains("미개봉 반품 기간", "미개봉은 7일 이내 반품", "반품은 7일 이내입니다.");
    }

    @Test
    void 근거와_주장을_평가하고_NO를_false로_변환한다() throws Exception {
        // Given: 가짜 평가 모델이 NO를 반환하도록 설정
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("NO"));

        // When: 근거와 후보 답변을 평가 API에 전달
        var response = post("facts", "미개봉 반품 기간", "미개봉은 7일 이내 반품", "30일 이내 반품입니다.");

        // Then: NO 응답의 false 변환과 실제 평가 프롬프트 검증
        assertThat(response.body()).isEqualTo("false");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        assertThat(MODEL.chat().getRequests().getFirst().path("messages").toString()).contains("Document:", "Claim:", "7일", "30일");
    }

    @Test
    void 예상하지_못한_평가_응답은_통과로_처리하지_않는다() throws Exception {
        // Given: YES/NO가 아닌 가짜 평가 응답 설정
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("maybe"));

        // When: API를 호출해 준비한 모델 응답 처리
        var response = post("relevance", "질문", "근거", "답변");

        // Then: 평가 성공으로 오인하지 않고 false를 반환하는지 검증
        assertThat(response.body()).isEqualTo("false");
    }

    @Test
    void 빈_후보_답변은_평가_모델_호출_전에_거부한다() throws Exception {
        assertThat(post("relevance", "질문", "근거", " ").statusCode()).isEqualTo(400);
        assertThat(MODEL.chat().getRequests()).isEmpty();
    }

    HttpResponse<String> post(String kind, String question, String context, String answer) throws Exception {
        var body = OpenAiStubServer.JSON
                .createObjectNode()
                .put("question", question)
                .put("context", context)
                .put("answer", answer);
        return appClient.postJson("/api/evaluate/" + kind, body.toString());
    }
}
