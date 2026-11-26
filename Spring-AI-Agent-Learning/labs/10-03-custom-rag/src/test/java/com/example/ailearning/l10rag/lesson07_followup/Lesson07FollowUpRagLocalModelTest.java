package com.example.ailearning.l10rag.lesson07_followup;

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
class Lesson07FollowUpRagLocalModelTest {
    static final OpenAiStubServer MODEL = createModelServer();
    @Autowired Lesson07FollowUpRagService service;

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
    void 이전_대화로_보충한_질문을_검색에_쓰고_원래_질문으로_답한다() {
        // Given: 검색 질문 변환과 답변 생성의 응답을 순서대로 준비한다.
        MODEL
                .chat()
                .setResponseSequence(
                createAssistantTextMessage("{\"question\":\"개봉한 상품의 반품 조건은 무엇인가요?\"}"),
                createAssistantTextMessage("{\"answerable\":true,\"answer\":\"개봉 상품은 단순 변심 반품이 불가하며 불량은 별도로 확인합니다.\",\"sourceIds\":[\"return-opened\"]}"));
        int embeddingRequestsBefore = MODEL.embeddings().getRequests().size();

        // When: 현재 질문만으로는 대상이 생략된 후속 문의를 보낸다.
        var result = service.answer("미개봉 상품의 반품 기간은?", "수령 후 7일 이내입니다.", "그럼 개봉했으면?");

        // Then: 검색에는 보충한 질문이, 생성에는 이전 대화와 원래 질문이 전달된다.
        System.out.println("검색 질문과 답변: " + result);
        var embeddingRequests = MODEL.embeddings().getRequests();
        assertThat(embeddingRequests).hasSize(embeddingRequestsBefore + 1);
        assertThat(embeddingRequests.getLast().path("input").toString()).contains("개봉한 상품의 반품 조건");
        assertThat(MODEL.chat().getRequests()).hasSize(2);
        String generationMessages = MODEL.chat().getRequests().getLast().path("messages").toString();
        assertThat(generationMessages).contains("그럼 개봉했으면?", "미개봉 상품의 반품 기간은?", "[return-opened]");
        assertThat(result.answer().status()).isEqualTo("ANSWERED");
        assertThat(result.answer().sources()).extracting(source -> source.id()).contains("return-opened");
    }

    @Test
    void 검색_질문이_비면_검색과_답변_생성을_진행하지_않는다() {
        MODEL
                .chat()
                .setResponseMessage(createAssistantTextMessage("{\"question\":\" \"}"));
        int embeddingRequestsBefore = MODEL.embeddings().getRequests().size();
        assertThatThrownBy(() -> service.answer("반품 기간은?", "7일입니다.", "개봉했으면?"))
                .isInstanceOf(IllegalStateException.class);
        System.out.println("빈 검색 질문은 IllegalStateException으로 거부");
        assertThat(MODEL.embeddings().getRequests()).hasSize(embeddingRequestsBefore);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    private static OpenAiStubServer createModelServer() {
        var server = new OpenAiStubServer();
        server
                .embeddings()
                .setVectorFunction(text -> text.contains("반품")
                ? new float[]{1, 0} : new float[]{0, 1});
        return server;
    }
}
