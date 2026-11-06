package com.example.ailearning.l06requests;

import com.example.ailearning.testsupport.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.import=", "spring.ai.openai.timeout=500ms", "spring.ai.openai.max-retries=0"})
class TimeoutNoRetryLocalModelTest {
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
    @Autowired ModelRequestController modelRequestController;

    @Test
    void 정상_응답은_한번의_요청으로_받는다() {
        MODEL
                .chat()
                .setResponseText("안녕하세요.");
        String answer = modelRequestService.ask("인사해줘.");
        System.out.println("답변: " + answer);
        assertThat(answer).isEqualTo("안녕하세요.");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    @Test
    void 늦은_모델_응답은_타임아웃으로_실패하고_재시도하지_않는다() {
        // Given: 응답 지연은 2초, 클라이언트 타임아웃은 500ms, 재시도는 0회
        MODEL
                .chat()
                .setResponseText("늦은 답변");
        MODEL
                .chat()
                .setResponseDelay(Duration.ofSeconds(2));

        // When: 실제 SDK가 로컬 모델 서버로 HTTP 요청
        Throwable error = catchThrowable(() -> modelRequestService.ask("인사해줘."));
        System.out.println("발생 예외: " + error);
        System.out.println("모델 서버가 받은 요청 수: " + MODEL.chat().getRequests().size());

        // Then: 서비스의 자체 Future가 아니라 SDK 통신 단계에서 실패
        assertThat(error).isInstanceOf(com.openai.errors.OpenAIIoException.class).hasRootCauseInstanceOf(java.io.InterruptedIOException.class);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    @Test
    void 재시도가_꺼져_있으면_429에도_추가_요청하지_않는다() {
        MODEL
                .chat()
                .setHttpStatus(429);
        Throwable error = catchThrowable(() -> modelRequestService.ask("인사해줘."));
        System.out.println("발생 예외: " + error);
        System.out.println("모델 서버가 받은 요청 수: " + MODEL.chat().getRequests().size());
        assertThat(error).isInstanceOf(com.openai.errors.RateLimitException.class);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    @Test
    void 모델_요청이_실패하면_컨트롤러는_500_응답을_지정한다() {
        MODEL
                .chat()
                .setHttpStatus(429);
        Throwable error = catchThrowable(() -> modelRequestController.ask("인사해줘."));
        System.out.println("컨트롤러 예외: " + error);
        assertThat(error).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(error.getCause()).isInstanceOf(com.openai.errors.RateLimitException.class);
    }
}
