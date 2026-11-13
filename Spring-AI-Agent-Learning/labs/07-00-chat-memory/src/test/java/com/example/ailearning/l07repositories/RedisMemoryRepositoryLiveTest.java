package com.example.ailearning.l07repositories;

import com.example.ailearning.testsupport.TestNamePrinter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 Redis Stack이 필요하다. 모델 API는 호출하지 않는다. */
class RedisMemoryRepositoryLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    @Timeout(30)
    void 같은_실행_중_InMemory와_Redis를_선택하고_대화가_분리됨을_확인한다() {
        new ApplicationContextRunner()
                .withUserConfiguration(SelectableMemoryConfiguration.class)
                .withPropertyValues("learning.redis.host=" + System.getenv().getOrDefault("LEARNING_REDIS_HOST", "127.0.0.1"),
                        "learning.redis.port=" + System.getenv().getOrDefault("LEARNING_REDIS_PORT", "6379"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var memoryService = context.getBean(SelectableMemoryService.class);
                    var localMemory = memoryService.memoryFor(MemoryStorage.IN_MEMORY);
                    var redisMemory = memoryService.memoryFor(MemoryStorage.REDIS);
                    String conversationId = "learning07:" + UUID.randomUUID();
                    try {
                        // Given: 로컬 저장소에만 이전 대화가 있음
                        localMemory.add(conversationId, new UserMessage("로컬 대화입니다."));
                        assertThat(redisMemory.get(conversationId)).isEmpty();

                        // When: 같은 실행 중 Redis를 골라 다른 대화 저장
                        redisMemory.add(conversationId, List.of(
                                new UserMessage("Redis 대화입니다."), new AssistantMessage("Redis에 저장했습니다.")));

                        // Then: 다시 선택해도 각 저장소의 대화가 독립적으로 유지됨
                        assertThat(memoryService.memoryFor(MemoryStorage.IN_MEMORY).get(conversationId))
                                .extracting(message -> message.getText())
                                .containsExactly("로컬 대화입니다.");
                        assertThat(memoryService.memoryFor(MemoryStorage.REDIS).get(conversationId))
                                .extracting(message -> message.getText())
                                .containsExactly("Redis 대화입니다.", "Redis에 저장했습니다.");
                        System.out.println("IN_MEMORY: " + localMemory.get(conversationId));
                        System.out.println("REDIS: " + redisMemory.get(conversationId));
                    } finally {
                        localMemory.clear(conversationId);
                        redisMemory.clear(conversationId);
                    }
                });
    }
}
