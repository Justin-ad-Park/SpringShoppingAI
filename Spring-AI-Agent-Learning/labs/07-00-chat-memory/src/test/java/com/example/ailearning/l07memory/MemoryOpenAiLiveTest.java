package com.example.ailearning.l07memory;

import com.example.ailearning.testsupport.TestNamePrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 OpenAI를 호출합니다. 저장소 루트 .env를 사용하며 API 사용량이 발생합니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.ai.openai.timeout=30s"
})
class MemoryOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired MemoryChatService service;

    @Test
    @Timeout(180)
    void 실제_OpenAI에_이전_대화와_새_질문을_함께_전달한다() {
        // Given: 첫 대화로 이름이 담긴 질문과 모델 답변 저장
        MemoryReply first = service.chat("live-user", "memory-demo", "내 이름은 민수야.");
        System.out.printf("첫 대화: %s%n", first);

        // When: 같은 사용자·대화 ID로 후속 질문 전달
        MemoryReply next = service.chat("live-user", "memory-demo", "내 이름이 뭐야?");
        System.out.printf("후속 대화와 전달 메시지: %s%n", next);

        // Then: 응답과 이전 대화 전달·저장 메시지 수 검증
        assertThat(first.answer()).isNotBlank();
        assertThat(next.answer()).isNotBlank();
        assertThat(next.sentMessages()).extracting(MessageView::role)
                .containsExactly("system", "user", "assistant", "user");
        // 모델이 생성한 문구를 고정하지 않고, 첫 응답이 다음 요청에 그대로 포함됐는지 확인합니다.
        assertThat(next.sentMessages()).extracting(MessageView::content)
                .containsSubsequence("내 이름은 민수야.", first.answer(), "내 이름이 뭐야?");
        assertThat(next.storedMessages()).isEqualTo(4);
    }
}
