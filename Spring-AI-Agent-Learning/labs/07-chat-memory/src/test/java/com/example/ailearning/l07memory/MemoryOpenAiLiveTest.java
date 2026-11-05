package com.example.ailearning.l07memory;

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
class MemoryOpenAiLiveTest {
    @Autowired MemoryChatService service;

    @Test
    @Timeout(180)
    void 실제_OpenAI에_이전_대화와_새_질문을_함께_전달한다(TestInfo info) {
        MemoryReply first = service.chat("live-user", "memory-demo", "내 이름은 민수야.");
        System.out.printf("[%s] 첫 대화: %s%n", info
                        .getTestMethod()
                        .orElseThrow()
                        .getName(), first);
        MemoryReply next = service.chat("live-user", "memory-demo", "내 이름이 뭐야?");
        System.out.printf("[%s] 후속 대화와 전달 메시지: %s%n", info
                        .getTestMethod()
                        .orElseThrow()
                        .getName(), next);
        assertThat(first.answer()).isNotBlank();
        assertThat(next.answer()).isNotBlank();
        assertThat(next.sentMessages()).hasSize(4);
        assertThat(next.storedMessages()).isEqualTo(4);
    }
}
