package com.example.ailearning.l07selection;

import com.example.ailearning.testsupport.TestNamePrinter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Redis Stack을 사용합니다. 테스트 전에 시작하고 반복 실행 동안 재사용하며, 실습을 마치면 종료합니다. 모델 API는 호출하지 않습니다.
 * Redis 실행·종료 명령은 저장소 루트 기준입니다. 기존 Docker Desktop을 먼저 실행합니다.
 * <pre>
 * bash Spring-AI-Agent-Learning/labs/07-03-Redis-Selection/runShell/start-redis.sh
 * bash Spring-AI-Agent-Learning/labs/07-03-Redis-Selection/runShell/stop-redis.sh
 * </pre>
 * 종료 스크립트는 컨테이너만 중지하고 데이터 볼륨은 보존합니다. 자세한 사용법은 runShell/README.md를 참고합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"test", "redis"})
class RedisMemoryRepositoryLiveTest {
    @Autowired ChatMemory chatMemory;
    @Autowired ChatMemoryRepository repository;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    @Timeout(30)
    void Redis_프로필로_선택한_저장소에_저장하고_조회하고_삭제한다() {
        String conversationId = UUID.randomUUID().toString();
        assertThat(repository).isInstanceOf(RedisChatMemoryRepository.class);
        try {
            chatMemory.add(conversationId, List.of(
                    new UserMessage("예산은 3만원이에요."), new AssistantMessage("예산을 확인했습니다.")));
            assertThat(chatMemory.get(conversationId)).extracting(message -> message.getText())
                    .containsExactly("예산은 3만원이에요.", "예산을 확인했습니다.");
            System.out.println("Redis 조회 결과: " + chatMemory.get(conversationId));
        } finally {
            // 테스트가 만든 대화만 삭제합니다. 데이터베이스나 다른 대화를 초기화하지 않습니다.
            chatMemory.clear(conversationId);
        }
        assertThat(chatMemory.get(conversationId)).isEmpty();
    }
}
