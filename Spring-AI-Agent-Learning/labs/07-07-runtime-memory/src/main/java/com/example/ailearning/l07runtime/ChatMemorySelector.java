package com.example.ailearning.l07runtime;

import java.util.Objects;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ChatMemorySelector {
    private final ChatMemory inMemoryChatMemory;
    private final ChatMemory redisChatMemory;

    public ChatMemorySelector(@Qualifier("inMemoryChatMemory") ChatMemory inMemoryChatMemory,
            @Qualifier("redisChatMemory") ChatMemory redisChatMemory) {
        this.inMemoryChatMemory = inMemoryChatMemory;
        this.redisChatMemory = redisChatMemory;
    }

    public ChatMemory select(MemoryStorage storage) {
        return switch (Objects.requireNonNull(storage, "저장소를 지정하세요.")) {
            case IN_MEMORY -> inMemoryChatMemory;
            case REDIS -> redisChatMemory;
        };
    }
}
