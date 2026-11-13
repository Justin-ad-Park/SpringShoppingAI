package com.example.ailearning.l07repositories;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** JDBC 구현체는 자동 구성으로 준비하고, 이 설정에서는 메시지 보관 개수만 정한다. */
@Configuration
public class JdbcMemoryConfiguration {
    @Bean
    public ChatMemory jdbcChatMemory(JdbcChatMemoryRepository jdbcRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcRepository)
                .maxMessages(6)
                .build();
    }
}
