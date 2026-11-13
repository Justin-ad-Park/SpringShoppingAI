package com.example.ailearning.l07selection;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.RedisClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MemoryProperties.class)
public class MemoryConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "learning.memory", name = "storage", havingValue = "in-memory")
    public ChatMemoryRepository inMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository repository, MemoryProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(properties.maxMessages())
                .build();
    }

    // Redis의 연결과 저장소는 Redis를 선택한 경우에만 생성합니다. 실패 시 InMemory로 숨겨 전환하지 않습니다.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "learning.memory", name = "storage", havingValue = "redis")
    static class RedisConfiguration {
        @Bean(destroyMethod = "close")
        RedisClient redisClient(MemoryProperties properties) {
            return RedisClient.builder()
                    .fromURI(properties.redis().uri())
                    .build();
        }

        @Bean
        ChatMemoryRepository redisRepository(RedisClient client, MemoryProperties properties) {
            var redis = properties.redis();
            return RedisChatMemoryRepository.builder()
                    .jedisClient(client)
                    .indexName(redis.indexName())
                    .keyPrefix(redis.keyPrefix())
                    .timeToLive(redis.timeToLive())
                    .initializeSchema(redis.initializeSchema())
                    .build();
        }
    }
}
