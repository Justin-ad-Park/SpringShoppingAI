package com.example.ailearning.l04structured;

import com.example.ailearning.l04structured.domain.ProductSearchCondition;

import com.example.ailearning.testsupport.TestNamePrinter;
import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.OpenAiStubServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class StructuredOutputLocalModelTest {

    // 테스트들이 공유하는 로컬 가짜 OpenAI 서버다.
    private static final OpenAiStubServer MODEL = new OpenAiStubServer();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private SearchConditionExtractor service;

    private final TestOutput output = new TestOutput();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        // 이전 테스트의 응답 설정과 요청 기록이 다음 테스트에 영향을 주지 않게 한다.
        MODEL
                .chat()
                .resetResponsesAndRequests();
        TestNamePrinter.printTestName(testInfo);
    }

    @AfterAll
    static void 테스트_종료() {
        MODEL.close();
    }

    @Test
    void JSON_응답을_record로_변환하고_형식_지시를_전달한다() throws Exception {
        // Given: 모델이 반환할 JSON 응답 설정
        MODEL
                .chat()
                .setResponseText("{\"category\":\"음료\",\"maxPrice\":15000}");

        // When: 챗 API를 호출해 JSON 응답을 record로 변환
        var response = postQuestion("음료 15000원 이하");

        // Then: 변환된 값과 모델에 전달한 형식 지시 검증
        assertThat(response.statusCode()).isEqualTo(200);
        var body = OpenAiStubServer.JSON.readTree(response.body());
        assertThat(body.path("category").asString()).isEqualTo("음료");
        assertThat(body.path("maxPrice").asInt()).isEqualTo(15000);
        assertThat(MODEL.chat().getRequests().getFirst().path("messages").toString()).contains("category", "maxPrice");
    }

    @Test
    void 사용자가_말하지_않은_조건은_null로_유지한다() throws Exception {
        // Given: 필드가 null인 모델 응답 설정
        MODEL
                .chat()
                .setResponseText("{\"category\":null,\"maxPrice\":null}");

        // When: 실제 서비스를 호출해 응답 변환
        ProductSearchCondition searchCondition = service.extractSearchCondition("선물 추천");
        output.printValue("조건 객체", searchCondition);

        // Then: 조건 객체의 필드가 null인지 검증
        assertThat(searchCondition.category()).isNull();
        assertThat(searchCondition.maxPrice()).isNull();
    }

    /** entity() 변환 예외를 서비스에서 ResponseStatusException으로 바꾸고 500를 지정하는 경로다. */
    @Test
    void 잘못된_JSON_응답은_변환_실패로_처리한다() throws Exception {
        // Given: JSON이 아닌 모델 응답 설정
        MODEL
                .chat()
                .setResponseText("이것은 JSON이 아닙니다.");

        // When: 챗 API 호출
        var response = postQuestion("음료 추천");

        // Then: 응답 변환 실패를 HTTP 500으로 처리하는지 검증
        assertThat(response.statusCode()).isEqualTo(500);
    }

    /** searchCondition.validate()의 IllegalArgumentException을 서비스에서 ResponseStatusException으로 바꾸고 422를 지정하는 경로다. */
    @Test
    void 변환에_성공해도_허용되지_않는_값은_거부한다() throws Exception {
        // Given: 모델이 반환할 음수 가격 설정
        MODEL
                .chat()
                .setResponseText("{\"category\":\"음료\",\"maxPrice\":-1}");

        // When: 음수 가격 응답을 반환하도록 설정한 챗 API 호출
        var negativePriceResponse = postQuestion("음료 추천");

        // Then: 값 검증 실패를 HTTP 422로 처리하는지 검증
        assertThat(negativePriceResponse.statusCode()).isEqualTo(422);

        // Given: 모델이 반환할 허용하지 않는 카테고리 설정
        MODEL
                .chat()
                .setResponseText("{\"category\":\"전자\",\"maxPrice\":1000}");

        // When: 허용하지 않는 카테고리 응답을 반환하도록 설정한 챗 API 호출
        var invalidCategoryResponse = postQuestion("추천");

        // Then: 값 검증 실패를 HTTP 422로 처리하는지 검증
        assertThat(invalidCategoryResponse.statusCode()).isEqualTo(422);
    }

    /** 질문의 @NotBlank 위반은 Spring MVC가 400으로 처리하므로 서비스·모델은 호출되지 않는다. */
    @Test
    void 공백_질문은_모델_호출_전에_거부한다() throws Exception {
        assertThat(postQuestion("   ").statusCode()).isEqualTo(400);
        assertThat(MODEL.chat().getRequests()).isEmpty();
    }

    private HttpResponse<String> postQuestion(String question) throws Exception {
        var request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/api/criteria"))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(question))
                .build();

        try (var http = HttpClient.newHttpClient()) {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            output.printHttpResponse(response);
            return response;
        }
    }
}
