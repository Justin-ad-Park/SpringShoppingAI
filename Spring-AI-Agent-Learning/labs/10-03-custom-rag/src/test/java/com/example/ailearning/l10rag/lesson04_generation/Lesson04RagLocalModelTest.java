package com.example.ailearning.l10rag.lesson04_generation;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import com.example.ailearning.testsupport.output.TestOutput;
import com.example.ailearning.testsupport.http.LocalTestHttpClient;
import com.example.ailearning.testsupport.model.OpenAiStubServer;
import static com.example.ailearning.testsupport.model.OpenAiChatStub.createAssistantTextMessage;
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
class Lesson04RagLocalModelTest {
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
    Lesson04RagService service;

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
        assertThat(body.path("sources").get(0).path("location").asString()).isEqualTo("policies/return-policies.md#return-sealed");
        String messages = MODEL
                .chat()
                .getRequests()
                .getFirst()
                .path("messages")
                .toString();
        output.printValue("채팅 모델에 전달한 질문과 정책 항목", messages);
        assertThat(messages).contains("REFERENCE_START", "미개봉 반품 기간", "return-sealed", "7일", "return-opened");
        assertThat(messages).doesNotContain("[shipping]", "[delivery-schedule]", "[refund]", "[coupon]", "무료배송", "쿠폰은 주문서");
        assertThat(MODEL.embeddings().getRequests().toString()).contains("미개봉", "반품");
    }

    @Test
    void 배송비_질문에는_관련_정책만_채팅_모델에_전달한다() throws Exception {
        // Given: 배송비 근거를 인용하는 모델 응답을 준비한다.
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage(
                "{\"answerable\":true,\"answer\":\"상품 금액 30000원 이상이면 무료배송입니다.\",\"sourceIds\":[\"shipping\"]}"));

        // When: 여러 정책이 들어 있는 문서에서 배송비를 문의한다.
        var response = post("무료배송은 얼마부터인가요?");

        // Then: 원본의 다른 항목은 보내지 않고, 배송비 계산에 필요한 조건은 함께 보낸다.
        String messages = MODEL
                .chat()
                .getRequests()
                .getFirst()
                .path("messages")
                .toString();
        output.printValue("채팅 모델에 전달한 질문과 정책 항목", messages);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(messages).contains("무료배송은 얼마부터인가요?", "[shipping]", "30000원", "할인 후 결제 금액", "합산");
        assertThat(messages).doesNotContain("[delivery-schedule]", "[return-sealed]", "[return-opened]", "[refund]", "[coupon]", "원래 결제한 수단");
    }

    @Test
    void 개봉_반품_질문에는_제한과_예외를_함께_전달한다() throws Exception {
        // Given: 질문에 필요한 반품 제한만 답한 모델 응답을 준비한다.
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage(
                "{\"answerable\":true,\"answer\":\"개봉한 상품은 단순 변심으로 반품할 수 없습니다.\",\"sourceIds\":[\"return-opened\"]}"));

        // When: 개봉한 상품의 단순 변심 반품 가능 여부를 문의한다.
        var response = post("상품을 개봉했는데 단순 변심으로 반품할 수 있나요?");

        // Then: Java는 예외 문장을 임의로 지우지 않고 해당 정책 항목을 통째로 전달한다.
        String messages = MODEL
                .chat()
                .getRequests()
                .getFirst()
                .path("messages")
                .toString();
        output.printValue("채팅 모델에 전달한 질문과 정책 항목", messages);
        var body = OpenAiStubServer.JSON.readTree(response.body());
        assertThat(body.path("status").asString()).isEqualTo("ANSWERED");
        assertThat(body.path("sources").get(0).path("id").asString()).isEqualTo("return-opened");
        assertThat(messages).contains("[return-opened]", "단순 변심으로 반품할 수 없습니다", "불량", "주문과 다른 상품");
        assertThat(messages).doesNotContain("[shipping]", "[delivery-schedule]", "[refund]", "[coupon]");
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
                    if (text.contains("배송비") || text.contains("무료배송") || text.contains("운송 요금")) {
                        return new float[]{1, 0, 0, 0, 0, 0};
                    }
                    if (text.contains("환불")) {
                        return new float[]{0, 0, 0, 1, 0, 0};
                    }
                    if (text.contains("반품") || text.contains("개봉")) {
                        return new float[]{0, 0, 1, 0, 0, 0};
                    }
                    if (text.contains("발송") || text.contains("배송")) {
                        return new float[]{0, 1, 0, 0, 0, 0};
                    }
                    if (text.contains("쿠폰")) {
                        return new float[]{0, 0, 0, 0, 1, 0};
                    }
                    return new float[]{0, 0, 0, 0, 0, 1};
                });
        return server;
    }
}
