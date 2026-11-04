package com.example.ailearning.l05tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 OpenAI를 호출합니다. 저장소 루트 .env를 사용하며 API 사용량이 발생합니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.ai.openai.base-url=https://api.openai.com/v1",
        "spring.ai.openai.chat.timeout=30s"
})
class ToolCallingOpenAiLiveTest {
    @Autowired ToolChatService service;

    @Test
    @Timeout(180)
    void 실제_OpenAI에_가격과_배송비를_질문한다(TestInfo info) {
        String answer = service.ask("P02 커피의 가격과 배송비 포함 총액을 도구로 확인해줘.");
        System.out.printf("[%s] 답변: %s%n", info.getTestMethod().orElseThrow().getName(), answer);
        assertThat(answer).isNotBlank();
    }
}
