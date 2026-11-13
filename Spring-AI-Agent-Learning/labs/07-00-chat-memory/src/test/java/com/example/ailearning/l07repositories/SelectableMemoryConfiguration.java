package com.example.ailearning.l07repositories;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.RedisClient;

/** 두 저장소를 동시에 준비하고 이름으로 구분해 연결한다. JDBC 예제와 별도로 실행한다. */
@Configuration
public class SelectableMemoryConfiguration {
    @Bean
    public InMemoryChatMemoryRepository inMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    @Bean(destroyMethod = "close")
    public RedisClient redisClient(@Value("${learning.redis.host:127.0.0.1}") String host,
            @Value("${learning.redis.port:6379}") int port) {
        return RedisClient.builder()
                .hostAndPort(host, port)
                .build();
    }

    @Bean
    public RedisChatMemoryRepository redisRepository(RedisClient redisClient) {
        return RedisChatMemoryRepository.builder()
                .jedisClient(redisClient)
                .indexName("learning07-memory")
                .keyPrefix("learning07-memory:")
                .initializeSchema(true)
                .build();
    }

    @Bean
    public ChatMemory inMemoryChatMemory(InMemoryChatMemoryRepository inMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(inMemoryRepository)
                .maxMessages(6)
                .build();
    }

    @Bean
    public ChatMemory redisChatMemory(RedisChatMemoryRepository redisRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisRepository)
                .maxMessages(6)
                .build();
    }

    @Bean
    public SelectableMemoryService selectableMemoryService(
            @Qualifier("inMemoryChatMemory") ChatMemory inMemoryChatMemory,
            @Qualifier("redisChatMemory") ChatMemory redisChatMemory) {
        return new SelectableMemoryService(inMemoryChatMemory, redisChatMemory);
    }
}
