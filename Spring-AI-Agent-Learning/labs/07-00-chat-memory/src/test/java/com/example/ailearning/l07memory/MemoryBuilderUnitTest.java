package com.example.ailearning.l07memory;

import com.example.ailearning.testsupport.TestNamePrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.UserMessage;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryBuilderUnitTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 기본_저장소는_빌더마다_별도로_생성된다() {
        // Given: 저장소를 지정하지 않고 메모리 두 개 생성
        var firstMemory = MessageWindowChatMemory.builder().maxMessages(6).build();
        var secondMemory = MessageWindowChatMemory.builder().maxMessages(6).build();

        // When: 첫 메모리에만 메시지 저장
        firstMemory.add("demo", new UserMessage("첫 메모리에만 저장합니다."));

        // Then: 같은 대화 ID여도 다른 빌더의 저장소에서는 보이지 않음
        assertThat(firstMemory.get("demo")).hasSize(1);
        assertThat(secondMemory.get("demo")).isEmpty();
        System.out.printf("첫 메모리: %d개, 두 번째 메모리: %d개%n",
                firstMemory.get("demo").size(), secondMemory.get("demo").size());
    }

    @Test
    void 같은_저장소를_명시적으로_연결하면_대화를_공유한다() {
        // Given: 하나의 저장소를 두 메모리에 전달
        var repository = new InMemoryChatMemoryRepository();
        var firstMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(6)
                .build();
        var secondMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(6)
                .build();

        // When: 첫 메모리에서 저장
        firstMemory.add("demo", new UserMessage("같은 저장소를 사용합니다."));

        // Then: 두 번째 메모리에서도 실제 저장한 내용을 조회
        assertThat(secondMemory.get("demo"))
                .extracting(message -> message.getText())
                .containsExactly("같은 저장소를 사용합니다.");
        System.out.println("첫 번째 메모리에서 조회: " + firstMemory.get("demo"));
        System.out.println("두 번째 메모리에서 조회: " + secondMemory.get("demo"));
    }
}
