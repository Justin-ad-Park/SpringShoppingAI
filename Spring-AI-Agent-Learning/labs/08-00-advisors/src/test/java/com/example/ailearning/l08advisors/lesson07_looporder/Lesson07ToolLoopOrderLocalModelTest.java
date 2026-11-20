package com.example.ailearning.l08advisors.lesson07_looporder;

import com.example.ailearning.testsupport.model.OpenAiStubServer;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static com.example.ailearning.testsupport.model.OpenAiChatStub.createAssistantTextMessage;
import static com.example.ailearning.testsupport.model.OpenAiChatStub.createAssistantToolCallMessage;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.config.import=")
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class Lesson07ToolLoopOrderLocalModelTest {
    private static final OpenAiStubServer MODEL = new OpenAiStubServer();

    @Autowired
    Lesson07ToolLoopOrderChatService service;

    @DynamicPropertySource
    static void modelAddress(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL.chat().resetResponsesAndRequests();
        MODEL.chat().setResponseText("고정 응답");
    }

    @AfterAll
    static void 모델_대역_종료() {
        MODEL.close();
    }

    @Test
    void 반복_앞의_Advisor는_한번_뒤의_Advisor는_모델_요청마다_실행된다(CapturedOutput logs) {
        MODEL.chat().setResponseSequence(
                createAssistantToolCallMessage("price-1", "getTeaPrice", "{}"),
                createAssistantTextMessage("허브티 가격은 12000원입니다."));

        String answer = service.chat("허브티 가격은 얼마인가요?");
        var steps = logs
                .getOut()
                .lines()
                .filter(line -> line.matches("(request|modelA|modelB):(before|after)"))
                .toList();
        System.out.println("답변: " + answer);

        assertThat(steps).containsExactly(
                "request:before",
                "modelA:before", "modelB:before", "modelB:after", "modelA:after",
                "modelA:before", "modelB:before", "modelB:after", "modelA:after",
                "request:after");
        assertThat(MODEL.chat().getRequests()).hasSize(2);
    }

    @Test
    void 도구_요청이_없으면_뒤의_Advisor도_한번만_실행된다(CapturedOutput logs) {
        String answer = service.chat("안녕하세요.");
        var steps = logs
                .getOut()
                .lines()
                .filter(line -> line.matches("(request|modelA|modelB):(before|after)"))
                .toList();
        System.out.println("답변: " + answer);
        assertThat(steps).containsExactly(
                "request:before",
                "modelA:before", "modelB:before", "modelB:after", "modelA:after",
                "request:after");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }
}
