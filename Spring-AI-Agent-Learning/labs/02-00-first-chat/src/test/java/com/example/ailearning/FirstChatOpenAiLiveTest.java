package com.example.ailearning;

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
 * 실제 OpenAI에 접속합니다. 루트 .env의 OPENAI_API_KEY, OPENAI_MODEL을 사용합니다.
 * 요청과 응답만 출력하며 API 키를 출력하지 않습니다.
 */
@DisplayName("첫 Chat 호출 — 실제 OpenAI 연결 테스트 (API 사용량 발생)")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FirstChatOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired
    private ChatService chatService;

    @Test
    @Timeout(60)
    @DisplayName("실제 OpenAI에 질문하고 응답을 출력한다")
    void 실제_OpenAI에_질문하고_응답을_출력한다() {
        String question = "내 이름은 Justin이야. 내 이름이 뭐야?";
        String answer = chatService.ask(question);

        System.out.println("=== 실제 OpenAI 응답 (MockModel 아님) ===");
        System.out.println("질문: " + question);
        System.out.println("응답: " + answer);
        // 모델의 문장은 매번 달라질 수 있으므로 고정 문장과 비교하지 않습니다.
        assertThat(answer).isNotBlank();
    }
}
