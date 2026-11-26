package com.example.ailearning.l13shopping.lesson09_completeness;

import com.example.ailearning.l13shopping.domain.*;
import com.example.ailearning.l13shopping.policy.PolicySearchResult;
import com.example.ailearning.l13shopping.workflow.CounselResult;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import java.util.List;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import com.example.ailearning.l13shopping.lesson02_intent.Lesson02CounselWorkflow;
import com.example.ailearning.testsupport.model.OpenAiStubServer;
import static com.example.ailearning.testsupport.model.OpenAiChatStub.createAssistantTextMessage;
import static org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.config.import=")
class Lesson09CompleteCounselLocalModelTest {
    static final OpenAiStubServer MODEL = createModelServer();
    @Autowired Lesson09CompleteCounselService service;
    // 기존 도구 흐름은 13-4에서 검증한다. 여기서는 의도적으로 정책이 빠진 결과를 준비한다.
    @MockitoBean Lesson02CounselWorkflow counselWorkflow;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.embedding.options.model", () -> "learning-embedding");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL
                .chat()
                .resetResponsesAndRequests();
    }

    @AfterAll
    static void 서버_종료() { MODEL.close(); }

    @Test
    void 모델이_추출한_두_요구와_상담_결과를_대조한다() {
        String question = "3만원 이하 음료를 추천하고 미개봉 반품 기간도 알려줘.";
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("{\"productRecommendation\":true,\"policyGuidance\":true}"));
        when(counselWorkflow.processCounselRequest(question, List.of())).thenReturn(productOnlyResult());

        var result = service.answer(question);

        System.out.println("요구 추출 후 누락 확인: " + result);
        assertThat(result.status()).isEqualTo("MISSING_RESULTS");
        assertThat(result.missingResults()).containsExactly("정책 안내");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        verify(counselWorkflow).processCounselRequest(question, List.of());
    }

    @Test
    void 처리할_요구를_못_찾으면_기본_상담을_호출하지_않는다() {
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("{\"productRecommendation\":false,\"policyGuidance\":false}"));
        assertThatThrownBy(() -> service.answer("안녕하세요.")).isInstanceOf(IllegalArgumentException.class);
        System.out.println("처리 범위 밖 요청은 기본 상담 전에 거부");
        verifyNoInteractions(counselWorkflow);
    }

    private static OpenAiStubServer createModelServer() {
        var server = new OpenAiStubServer();
        server
                .embeddings()
                .setVectorFunction(text -> new float[]{1, 0});
        return server;
    }

    private static CounselResult productOnlyResult() {
        var product = new Product("P01", "허브티", "음료", 12000);
        var priceDetail = new ProductPriceDetail(product, 3000, 15000);
        return new CounselResult("VERIFIED", "허브티 총 15000원", new Intent("SHOPPING", "음료", 30000),
                List.of(priceDetail), List.of());
    }
}
