package com.example.ailearning.l02requests;

import com.example.ailearning.testsupport.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.*;

/**
 * 같은 ask() 호출에 대해 OpenAI SDK의 오류별 자동 재시도 정책을 확인한다.
 * max-retries=1은 추가 시도 최대 1회다. 429·500이 계속 오면 총 2회 요청하고,
 * 재시도 대상이 아닌 400이나 정상 응답에는 최초 요청 1회만 전송한다.
 * 로컬 서버는 고정 응답을 반환하며 실제 외부 모델은 호출하지 않는다.
 */
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

    // 429는 일시적인 요청 한도 초과일 수 있어 재시도한다. 대역은 두 번째에도 429를 반환한다.
    @Test
    void 계속되는_429는_한번_재시도해_총_두번_요청한다() {
        MODEL
                .chat()
                .setHttpStatus(429);
        Throwable error = catchThrowable(() -> modelRequestService.ask("인사해줘."));
        System.out.println("재시도 후 예외: " + error);
        System.out.println("총 HTTP 요청 수(최초 요청 포함): " + MODEL.chat().getRequests().size());
        assertThat(error).isInstanceOf(com.openai.errors.RateLimitException.class);
        // 최초 요청 1회 + 추가 재시도 1회. 두 번 모두 실패해 예외가 전달된다.
        assertThat(MODEL.chat().getRequests()).hasSize(2);
    }

    // 500도 일시적인 서버 오류일 수 있어 재시도한다. 429와 동일하게 총 요청은 2회다.
    @Test
    void 계속되는_500은_한번_재시도해_총_두번_요청한다() {
        MODEL
                .chat()
                .setHttpStatus(500);
        Throwable error = catchThrowable(() -> modelRequestService.ask("인사해줘."));
        System.out.println("재시도 후 예외: " + error);
        System.out.println("총 HTTP 요청 수(최초 요청 포함): " + MODEL.chat().getRequests().size());
        assertThat(error).isInstanceOf(com.openai.errors.InternalServerException.class);
        // 최초 요청 1회 + 추가 재시도 1회. 두 번 모두 실패해 예외가 전달된다.
        assertThat(MODEL.chat().getRequests()).hasSize(2);
    }

    // 400은 요청 내용을 고쳐야 하므로 같은 요청을 자동으로 다시 보내지 않는다.
    @Test
    void 잘못된_요청인_400은_재시도_설정이_있어도_다시_보내지_않는다() {
        MODEL
                .chat()
                .setHttpStatus(400);
        Throwable error = catchThrowable(() -> modelRequestService.ask("인사해줘."));
        System.out.println("발생 예외: " + error);
        System.out.println("총 HTTP 요청 수(최초 요청 포함): " + MODEL.chat().getRequests().size());
        assertThat(error).isInstanceOf(com.openai.errors.BadRequestException.class);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }

    // 재시도 설정은 실패 시의 상한이며, 성공한 요청을 반복 실행하는 설정이 아니다.
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
