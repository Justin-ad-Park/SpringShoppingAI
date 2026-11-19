package com.example.ailearning.l08advisors.toolloop;

import com.example.ailearning.testsupport.http.LocalTestHttpClient;
import com.example.ailearning.testsupport.model.OpenAiStubServer;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import com.example.ailearning.testsupport.output.TestOutput;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.ailearning.testsupport.output.ToolCallPrinter;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static com.example.ailearning.testsupport.model.OpenAiChatStub.createAssistantTextMessage;
import static com.example.ailearning.testsupport.model.OpenAiChatStub.createAssistantToolCallMessage;
import static org.mockito.Mockito.*;

/** 08-5의 전용 로컬 테스트. 실습 앱과 Spring AI를 실행하고 모델 HTTP 응답만 고정한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.config.import=")
@ActiveProfiles("test")
class Lesson05ToolLoopAdvisorLocalModelTest {
    private static final OpenAiStubServer MODEL = new OpenAiStubServer();
    private final TestOutput output = new TestOutput();

    @Value("${local.server.port}")
    int port;

    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> this.port, output);

    @DynamicPropertySource
    static void modelAddress(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL
                .chat()
                .resetResponsesAndRequests();
        MODEL
                .chat()
                .setResponseText("고정 응답");
    }

    @AfterAll
    static void 모델_대역_종료() {
        MODEL.close();
    }

    @MockitoSpyBean
    PriceTools priceTools;

    @Test
    void 도구를_한번_실행하면_전체_요청은_한번_모델_요청은_두번_기록한다() throws Exception {
        // Given: 모델이 도구 요청과 최종 답변을 순서대로 반환
        MODEL
                .chat()
                .setResponseSequence(
                createAssistantToolCallMessage("price-1", "getTeaPrice", "{}"),
                createAssistantTextMessage("허브티 가격은 12000원입니다."));

        // When
        var response = appClient.postText("/api/advisors/tool-loop", "허브티 가격은 얼마인가요?");
        ToolCallPrinter.printToolCalls(priceTools);

        // Then: 문자열 응답이 아닌 실제 도구 실행과 모델에 전달된 결과도 확인
        assertThat(response.statusCode()).isEqualTo(200);
        verify(priceTools).getTeaPrice();
        assertThat(MODEL.chat().getRequests()).hasSize(2);
        assertThat(MODEL.chat().getRequests().getLast().path("messages").toString()).contains("price-1", "12000", "tool");
        var reply = OpenAiStubServer.JSON.readValue(response.body(), Lesson05ToolLoopChatService.Reply.class);
        assertThat(reply.steps())
                .containsExactly("request:before", "model:before", "model:after", "model:before", "model:after", "request:after");
    }

    @Test
    void 도구를_등록해도_모델이_도구를_요청하지_않으면_루프를_반복하지_않는다() throws Exception {
        // Given
        MODEL
                .chat()
                .setResponseText("안녕하세요.");

        // When
        var response = appClient.postText("/api/advisors/tool-loop", "안녕하세요.");

        // Then
        verifyNoInteractions(priceTools);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        var reply = OpenAiStubServer.JSON.readValue(response.body(), Lesson05ToolLoopChatService.Reply.class);
        assertThat(reply.steps())
                .containsExactly("request:before", "model:before", "model:after", "request:after");
    }
}
