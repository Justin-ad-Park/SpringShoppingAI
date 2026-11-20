package com.example.ailearning.l08advisors.lesson05_order;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.config.import=")
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class Lesson05AdvisorOrderLocalModelTest {
    private static final OpenAiStubServer MODEL = new OpenAiStubServer();

    @Autowired
    Lesson05AdvisorOrderChatService service;

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
    void 나열_순서보다_order가_작은_Advisor부터_진입한다(CapturedOutput logs) {
        String answer = service.chat("안녕하세요.");
        var steps = logs.getOut().lines().filter(line -> line.matches("[AB]:(before|after)")).toList();
        System.out.println("답변: " + answer);
        assertThat(steps).containsExactly("A:before", "B:before", "B:after", "A:after");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
    }
}
