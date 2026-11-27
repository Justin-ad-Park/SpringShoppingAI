package com.example.ailearning.l13shopping.lesson10_shoppingandreturn;

import static com.example.ailearning.testsupport.model.OpenAiChatStub.createAssistantTextMessage;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.ailearning.testsupport.model.OpenAiStubServer;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.config.import=")
class Lesson10ShoppingAndReturnLocalModelTest {
    private static final String COMBINED_QUESTION = "3만원 이하 음료를 추천하고 미개봉 반품 기간도 알려줘.";
    private static final OpenAiStubServer MODEL = startModelServer();

    @Autowired
    Lesson10ShoppingAndReturnService service;

    @DynamicPropertySource
    static void 모델_접속_설정(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.embedding.options.model", () -> "learning-embedding");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL.chat().resetResponsesAndRequests();
        MODEL.embeddings().setVectorFunction(Lesson10ShoppingAndReturnLocalModelTest::policyVector);
    }

    @AfterAll
    static void 서버_종료() {
        MODEL.close();
    }

    @Test
    void 예산에_맞는_상품과_미개봉_반품_기간을_함께_답한다() {
        setExtractedInputs("""
                {"category":"음료","totalBudget":30000,"returnCondition":"SEALED"}
                """);

        var result = service.answer(COMBINED_QUESTION);

        System.out.println(result.answer());
        assertThat(result.status()).isEqualTo("COMPLETE");
        assertThat(result.recommendation().product().id()).isEqualTo("P01");
        assertThat(result.recommendation().total()).isEqualTo(15000);
        assertThat(result.returnPolicy().id()).isEqualTo("return-sealed");
        assertThat(result.answer()).contains("허브티", "총 15000원", "수령 후 7일 이내");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    @Test
    void 모델이_개봉_상태를_빠뜨리면_완료_대신_다시_묻는다() {
        setExtractedInputs("""
                {"category":"음료","totalBudget":30000}
                """);
        int embeddingRequestsBefore = MODEL.embeddings().getRequests().size();

        var result = service.answer(COMBINED_QUESTION);

        System.out.println(result.answer());
        assertThat(result.status()).isEqualTo("NEEDS_INPUT");
        assertThat(result.answer()).contains("개봉했는지");
        assertThat(result.returnPolicy()).isNull();
        assertThat(MODEL.embeddings().getRequests()).hasSize(embeddingRequestsBefore);
    }

    @Test
    void 미개봉_정책이_검색되지_않으면_찾은_상품만_안내한다() {
        setExtractedInputs("""
                {"category":"음료","totalBudget":30000,"returnCondition":"SEALED"}
                """);
        // 정책 적재는 이미 끝났다. 지금부터는 검색어가 배송비 문서에만 가깝도록 응답한다.
        MODEL.embeddings().setVectorFunction(text -> new float[]{0, 0, 1});

        var result = service.answer(COMBINED_QUESTION);

        System.out.println(result.answer());
        assertThat(result.status()).isEqualTo("MISSING_RESULTS");
        assertThat(result.recommendation().product().id()).isEqualTo("P01");
        assertThat(result.returnPolicy()).isNull();
        assertThat(result.answer()).contains("반품 정책을 확인하지 못했습니다").doesNotContain("7일");
    }

    @Test
    void 상품가가_예산에_맞아도_배송비_포함_총액이_넘으면_추천하지_않는다() {
        setExtractedInputs("""
                {"category":"음료","totalBudget":12000,"returnCondition":"OPENED"}
                """);

        var result = service.answer("1만2천원 이하 음료를 추천하고 개봉한 상품도 반품이 되는지 알려줘.");

        System.out.println(result.answer());
        assertThat(result.status()).isEqualTo("MISSING_RESULTS");
        assertThat(result.recommendation()).isNull();
        assertThat(result.returnPolicy().id()).isEqualTo("return-opened");
        assertThat(result.answer()).contains("단순 변심 반품이 불가합니다");
    }

    private static void setExtractedInputs(String json) {
        MODEL.chat().setResponseMessage(createAssistantTextMessage(json));
    }

    private static OpenAiStubServer startModelServer() {
        var server = new OpenAiStubServer();
        server.embeddings().setVectorFunction(Lesson10ShoppingAndReturnLocalModelTest::policyVector);
        return server;
    }

    private static float[] policyVector(String text) {
        if (text.contains("미개봉")) {
            return new float[]{1, 0, 0};
        }
        if (text.contains("개봉한")) {
            return new float[]{0, 1, 0};
        }
        return new float[]{0, 0, 1};
    }
}
