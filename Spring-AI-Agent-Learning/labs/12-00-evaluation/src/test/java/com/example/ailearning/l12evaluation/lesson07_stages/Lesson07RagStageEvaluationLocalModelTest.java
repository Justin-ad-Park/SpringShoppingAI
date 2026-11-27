package com.example.ailearning.l12evaluation.lesson07_stages;

import com.example.ailearning.testsupport.model.OpenAiStubServer;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import static com.example.ailearning.testsupport.model.OpenAiChatStub.createAssistantTextMessage;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.config.import=")
class Lesson07RagStageEvaluationLocalModelTest {
    static final OpenAiStubServer MODEL = createModelServer();
    @Autowired Lesson07RagStageEvaluationService service;

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

    @BeforeEach
    void 기본_벡터_설정() {
        MODEL
                .embeddings()
                .setVectorFunction(Lesson07RagStageEvaluationLocalModelTest::policyVector);
    }

    @Test
    void 필요한_문서를_못_찾으면_검색_실패로_판정하고_생성을_생략한다() {
        // 문서는 이미 적재됐다. 질문 벡터만 배송 정책 쪽으로 틀어 오검색을 재현한다.
        MODEL
                .embeddings()
                .setVectorFunction(text -> new float[]{1, 0});
        var result = service.evaluate("미개봉 반품 기간은?", List.of("return-sealed"), "7일");
        System.out.println("검색 실패: " + result);
        assertThat(result.retrievedIds()).contains("shipping").doesNotContain("return-sealed");
        assertThat(result.retrievalPassed()).isFalse();
        assertThat(result.generationEvaluated()).isFalse();
        assertThat(MODEL.chat().getRequests()).isEmpty();
    }

    @Test
    void 필요한_문서를_찾아도_답변에_필수_문구가_없으면_검사에서_실패한다() {
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("미개봉 반품은 30일 이내입니다."));
        var result = service.evaluate("미개봉 반품 기간은?", List.of("return-sealed"), "7일");
        System.out.println("검색 통과·필수 문구 누락: " + result);
        assertThat(result.retrievalPassed()).isTrue();
        assertThat(result.generationEvaluated()).isTrue();
        assertThat(result.generationFailures()).containsExactly("필수 문구 누락: 7일");
        assertThat(MODEL.chat().getRequests().getFirst().path("messages").toString()).contains("return-sealed", "7일");
    }

    @Test
    void 필요한_문서와_필수_답변_문구를_각각_확인한다() {
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("미개봉 상품은 수령 후 7일 이내 반품할 수 있습니다."));
        var result = service.evaluate("미개봉 반품 기간은?", List.of("return-sealed"), "7일");
        System.out.println("두 단계 검사 통과: " + result);
        assertThat(result.retrievalPassed()).isTrue();
        assertThat(result.generationEvaluated()).isTrue();
        assertThat(result.generationFailures()).isEmpty();
    }

    @Test
    void 필수_문구를_포함한_오답은_문구_검사를_통과한다() {
        String incorrectAnswer = "미개봉 반품은 7일이 아니라 30일 이내입니다.";
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage(incorrectAnswer));

        var result = service.evaluate("미개봉 반품 기간은?", List.of("return-sealed"), "7일");

        System.out.println("검색과 문구 검사를 모두 통과한 오답: " + result);
        assertThat(result.retrievedIds()).contains("return-sealed");
        assertThat(result.retrievalPassed()).isTrue();
        assertThat(result.generationEvaluated()).isTrue();
        assertThat(result.answer()).isEqualTo(incorrectAnswer);
        assertThat(result.generationFailures()).isEmpty();
    }

    @Test
    void 기산점을_틀려도_7일이_있으면_문구_검사를_통과한다() {
        String answer = "미개봉 상품은 주문 후 7일 이내 반품할 수 있습니다.";

        var result = evaluateWithAnswer(answer);

        assertThat(result.generationFailures()).isEmpty();
    }

    @Test
    void 정책과_같은_뜻이어도_일주일은_7일_문구_검사에서_실패한다() {
        String answer = "미개봉 상품은 수령 후 일주일 이내 반품할 수 있습니다.";

        var result = evaluateWithAnswer(answer);

        assertThat(result.generationFailures()).containsExactly("필수 문구 누락: 7일");
    }

    private Lesson07RagStageEvaluationService.Result evaluateWithAnswer(String answer) {
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage(answer));

        var result = service.evaluate("미개봉 반품 기간은?", List.of("return-sealed"), "7일");

        assertThat(result.retrievedIds()).contains("return-sealed");
        assertThat(result.retrievalPassed()).isTrue();
        assertThat(result.generationEvaluated()).isTrue();
        assertThat(result.answer()).isEqualTo(answer);
        // 찾은 문서 ID와 실제 user 메시지의 정책 본문을 함께 확인한다.
        var request = MODEL.chat().getRequests().getFirst();
        var userMessage = request.path("messages")
                .valueStream()
                .filter(message -> message.path("role").asString().equals("user"))
                .findFirst()
                .orElseThrow();
        String policyInRequest = userMessage.path("content").asString();
        System.out.println("모델에 보낸 질문과 정책: " + policyInRequest);
        System.out.println("검사 결과: " + result);
        assertThat(policyInRequest).contains("[return-sealed] 미개봉 상품은 수령 후 7일 이내 반품할 수 있습니다.");
        return result;
    }

    private static float[] policyVector(String text) {
        return text.contains("반품") ? new float[]{0, 1} : new float[]{1, 0};
    }

    private static OpenAiStubServer createModelServer() {
        var server = new OpenAiStubServer();
        server
                .embeddings()
                .setVectorFunction(Lesson07RagStageEvaluationLocalModelTest::policyVector);
        return server;
    }
}
