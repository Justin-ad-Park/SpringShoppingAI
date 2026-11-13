package com.example.ailearning.l07runtime;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.RedisClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MemoryProperties.class)
public class MemoryConfiguration {
    @Bean
    public ChatMemory inMemoryChatMemory(MemoryProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(properties.maxMessages())
                .build();
    }

    @Bean(destroyMethod = "close")
    public RedisClient redisClient(MemoryProperties properties) {
        return RedisClient.builder()
                .fromURI(properties.redis().uri())
                .build();
    }

    @Bean
    public ChatMemory redisChatMemory(RedisClient client, MemoryProperties properties) {
        var settings = properties.redis();
        var repository = RedisChatMemoryRepository.builder()
                .jedisClient(client)
                .indexName(settings.indexName())
                .keyPrefix(settings.keyPrefix())
                .timeToLive(settings.timeToLive())
                .initializeSchema(settings.initializeSchema())
                .build();
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(properties.maxMessages())
                .build();
    }
}
