package com.example.ailearning.l06requests;

import com.example.ailearning.testsupport.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.import=", "spring.ai.openai.timeout=2s", "spring.ai.openai.max-retries=1"})
class RetryLocalModelTest {
    static final OpenAiStubServer MODEL = new OpenAiStubServer();
    @Autowired ModelRequestService modelRequestService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
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
    static void 종료() { MODEL.close(); }

    @Test
    void 재시도_한번이면_계속되는_429에_총_두번_요청한다() {
        MODEL
                .chat()
                .setHttpStatus(429);
        Throwable error = catchThrowable(() -> modelRequestService.ask("인사해줘."));
        System.out.println("재시도 후 예외: " + error);
        System.out.println("모델 서버가 받은 요청 수: " + MODEL.chat().getRequests().size());
        assertThat(error).isInstanceOf(com.openai.errors.RateLimitException.class);
        assertThat(MODEL.chat().getRequests()).hasSize(2);
    }

    @Test
    void 재시도_가능한_서버_오류도_한번만_재시도한다() {
        MODEL
                .chat()
                .setHttpStatus(500);
        Throwable error = catchThrowable(() -> modelRequestService.ask("인사해줘."));
        System.out.println("재시도 후 예외: " + error);
        System.out.println("모델 서버가 받은 요청 수: " + MODEL.chat().getRequests().size());
        assertThat(error).isInstanceOf(com.openai.errors.InternalServerException.class);
        assertThat(MODEL.chat().getRequests()).hasSize(2);
    }

    @Test
    void 잘못된_요청인_400은_재시도_설정이_있어도_다시_보내지_않는다() {
        MODEL
                .chat()
                .setHttpStatus(400);
        Throwable error = catchThrowable(() -> modelRequestService.ask("인사해줘."));
        System.out.println("발생 예외: " + error);
        System.out.println("모델 서버가 받은 요청 수: " + MODEL.chat().getRequests().size());
        assertThat(error).isInstanceOf(com.openai.errors.BadRequestException.class);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    @Test
    void 정상_응답에는_재시도_설정이_있어도_추가_요청하지_않는다() {
        MODEL
                .chat()
                .setResponseText("안녕하세요.");
        String answer = modelRequestService.ask("인사해줘.");
        System.out.println("답변: " + answer);
        assertThat(answer).isEqualTo("안녕하세요.");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }
}
