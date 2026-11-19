package com.example.ailearning.l08advisors.logging;

import com.example.ailearning.testsupport.http.LocalTestHttpClient;
import com.example.ailearning.testsupport.model.OpenAiStubServer;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import com.example.ailearning.testsupport.output.TestOutput;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** 08-1: 직접 Advisor를 구현하지 않고 기본 로거의 실제 요청·응답 기록을 확인한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.config.import=")
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class Lesson01LoggingLocalModelTest {
    private static final OpenAiStubServer MODEL = new OpenAiStubServer();

    @Value("${local.server.port}")
    int port;

    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> this.port, new TestOutput());

    @DynamicPropertySource
    static void modelAddress(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL.chat().resetResponsesAndRequests();
        MODEL.chat().setResponseText("상품 문의를 도와드립니다.");
    }

    @AfterAll
    static void 모델_대역_종료() {
        MODEL.close();
    }

    @Test
    void 기본_로거_Advisor가_모델에_보낸_질문과_받은_답변을_기록한다(CapturedOutput logs) throws Exception {
        // When: 실제 Controller·Service·Spring AI를 실행하고 모델 응답만 고정
        var response = appClient.postText("/api/advisors/logging", "어떤 상품 문의를 할 수 있나요?");

        // Then: 직접 구현한 Advisor 없이 기본 제공 로거가 동작하는지 확인
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("상품 문의를 도와드립니다.");
        assertThat(logs.getOut()).contains("request: ", "어떤 상품 문의를 할 수 있나요?", "response: ");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }
}
