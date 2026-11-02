package com.example.ailearning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 OpenAI에 접속합니다. 루트 .env의 OPENAI_API_KEY, OPENAI_MODEL을 사용합니다.
 * 일반 test에서는 제외됩니다. liveOpenAiTest 또는 이 클래스를 명시한 IntelliJ Run은 API 사용량이 발생합니다.
 * 요청과 응답만 출력하며 API 키를 출력하지 않습니다.
 */
@Tag("live-openai")
@DisplayName("첫 Chat 호출 — 실제 OpenAI 연결 테스트 (API 사용량 발생)")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.ai.openai.base-url=https://api.openai.com/v1")
class FirstChatOpenAiLiveTest {
    @Autowired
    private ChatService chatService;

    @Test
    @Timeout(60)
    @DisplayName("실제 OpenAI에 질문하고 응답을 출력한다")
    void callsRealOpenAiAndPrintsResponse() {
        String question = "Spring의 의존성 주입을 초보자에게 한국어 두 문장으로 설명해 주세요.";
        String answer = chatService.ask(question);

        System.out.println("=== 실제 OpenAI 응답 (MockModel 아님) ===");
        System.out.println("질문: " + question);
        System.out.println("응답: " + answer);
        // 모델의 문장은 매번 달라질 수 있으므로 고정 문장과 비교하지 않습니다.
        assertThat(answer).isNotBlank();
    }
}
