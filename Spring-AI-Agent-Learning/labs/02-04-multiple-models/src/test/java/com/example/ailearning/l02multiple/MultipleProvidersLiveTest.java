package com.example.ailearning.l02multiple;

import com.example.ailearning.testsupport.TestNamePrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 루트 .env의 OPENAI_API_KEY, OPENAI_MODEL, ANTHROPIC_API_KEY, ANTHROPIC_MODEL을 사용합니다.
 * 로컬 가짜 서버를 사용하지 않으며 실제 API 사용량이 발생합니다. 키는 출력하지 않습니다.
 */
@DisplayName("다중 모델 — 실제 OpenAI·Anthropic 연결 테스트 (API 사용량 발생)")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MultipleProvidersLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired
    private MultipleModelsService service;

    @Test
    @Timeout(60)
    @DisplayName("실제 OpenAI에 질문하고 응답을 출력한다")
    void 실제_OpenAI에_질문하고_응답을_출력한다() {
        String question = "Spring의 의존성 주입을 쉬운 한국어 한 문장으로 설명해줘.";
        String answer = service.askOpenAi(question);

        System.out.println("=== 실제 OpenAI 응답 (로컬 고정 응답 아님) ===");
        System.out.println("질문: " + question);
        System.out.println("응답: " + answer);
        assertThat(answer).isNotBlank();
    }

//    @Test
//    @Timeout(60)
//    @DisplayName("실제 Anthropic에 질문하고 응답을 출력한다")
//    void 실제_Anthropic에_질문하고_응답을_출력한다() {
//        String question = "Spring의 의존성 주입을 쉬운 한국어 한 문장으로 설명해줘.";
//        String answer = service.askAnthropic(question);
//
//        System.out.println("=== 실제 Anthropic 응답 (로컬 고정 응답 아님) ===");
//        System.out.println("질문: " + question);
//        System.out.println("응답: " + answer);
//        assertThat(answer).isNotBlank();
//    }
}
