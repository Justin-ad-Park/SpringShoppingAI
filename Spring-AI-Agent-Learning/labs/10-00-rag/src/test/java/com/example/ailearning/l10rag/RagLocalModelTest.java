package com.example.ailearning.l10rag;

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
class RagLocalModelTest {
    private final TestOutput output = new TestOutput();

    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> this.port, output);

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    static final OpenAiStubServer MODEL = createModelServer();
    @Value("${local.server.port}")
    int port;

    @org.springframework.beans.factory.annotation.Autowired
    RagService service;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.embedding.options.model", () -> "learning-embedding");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void reset() {
        MODEL
                .chat()
                .resetResponsesAndRequests();
    }

    @AfterAll
    static void stop() {
        MODEL.close();
    }

    @Test
    void 문서를_나누어_검색한_근거를_모델에_전달한다() throws Exception {
        // Given: 근거 ID를 포함한 가짜 모델 답변 설정
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("{\"answerable\":true,\"answer\":\"미개봉은 7일 이내입니다.\",\"sourceIds\":[\"return-sealed\"]}"));

        // When: 검색과 답변 생성을 수행하는 RAG API 호출
        var response = post("미개봉 반품 기간");

        // Then: 검색한 문서의 출처와 내용이 모델 요청에 들어갔는지 검증
        assertThat(response.statusCode()).isEqualTo(200);
        var body = OpenAiStubServer.JSON.readTree(response.body());
        assertThat(body.path("status").asString()).isEqualTo("ANSWERED");
        assertThat(body.path("sources").get(0).path("location").asString()).isEqualTo("policies.md#return-sealed");
        String messages = MODEL
                .chat()
                .getRequests()
                .getFirst()
                .path("messages")
                .toString();
        assertThat(messages).contains("REFERENCE_START", "return-sealed", "7일", "return-opened");
        assertThat(MODEL.embeddings().getRequests().toString()).contains("미개봉", "반품");
    }

    @Test
    void 검색_근거가_없으면_답변_모델을_호출하지_않는다() throws Exception {
        var response = post("우주 여행");
        assertThat(OpenAiStubServer.JSON.readTree(response.body()).path("status").asString()).isEqualTo("NO_CONTEXT");
        assertThat(MODEL.chat().getRequests()).isEmpty();
    }

    @Test
    void 검색하지_않은_출처나_출처_누락은_답변으로_채택하지_않는다() throws Exception {
        // Given: 검색하지 않은 출처를 인용하는 모델 응답 설정
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("{\"answerable\":true,\"answer\":\"아무 주장\",\"sourceIds\":[\"invented\"]}"));

        // When: 준비한 모델 응답으로 RAG API 실행
        var unknownSource = post("반품");

        // Then: 검색하지 않은 출처 ID을 근거로 채택하지 않는지 검증
        assertThat(OpenAiStubServer.JSON.readTree(unknownSource.body()).path("status").asString()).isEqualTo("INVALID_CITATION");

        // Given: 출처를 누락한 모델 응답으로 변경
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("{\"answerable\":true,\"answer\":\"아무 주장\",\"sourceIds\":[]}"));

        // When: 준비한 모델 응답으로 RAG API 실행
        var missingSource = post("반품");

        // Then: 출처 ID가 비어 있는 응답을 근거로 채택하지 않는지 검증
        assertThat(OpenAiStubServer.JSON.readTree(missingSource.body()).path("status").asString()).isEqualTo("INVALID_CITATION");
    }

    @Test
    void 문서를_찾았어도_근거가_부족하면_답변을_보류한다() throws Exception {
        // Given: 검색 후 답할 수 없다는 가짜 모델 응답 설정
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("{\"answerable\":false,\"answer\":\"모릅니다\",\"sourceIds\":[]}"));

        // When: API를 호출해 준비한 모델 응답 처리
        var response = post("반품 담당자 이름");

        // Then: 근거 부족 상태로 답변을 보류하는지 검증
        assertThat(OpenAiStubServer.JSON.readTree(response.body()).path("status").asString()).isEqualTo("NO_ANSWER");
    }

    @Test
    void 답변_형식이_잘못되면_변환_실패로_처리한다() throws Exception {
        // Given: JSON이 아닌 가짜 모델 응답 설정
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("형식 오류"));

        // When: API를 호출해 준비한 모델 응답 처리
        var response = post("반품");

        // Then: 모델 응답 변환 실패를 HTTP 500으로 처리하는지 검증
        assertThat(response.statusCode()).isEqualTo(500);
    }

    @Test
    void 공백_질문은_답변_모델을_호출하지_않는다() throws Exception {
        assertThat(post(" ").statusCode()).isEqualTo(400);
        assertThat(MODEL.chat().getRequests()).isEmpty();
    }

    HttpResponse<String> post(String question) throws Exception {
        return appClient.postText("/api/rag", question);
    }

    private static OpenAiStubServer createModelServer() {
        var server = new OpenAiStubServer();
        server
                .embeddings()
                .setVectorFunction(text -> {
                    if (text.contains("배송") || text.contains("운송")) {
                        return new float[]{1, 0, 0};
                    }
                    if (text.contains("반품") || text.contains("개봉")) {
                        return new float[]{0, 1, 0};
                    }
                    return new float[]{0, 0, 1};
                });
        return server;
    }
}
