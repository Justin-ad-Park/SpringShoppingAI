package com.example.ailearning.l07repositories;

import java.util.Map;
import java.util.Objects;
import org.springframework.ai.chat.memory.ChatMemory;

/** 요청마다 사용할 저장소를 선택한다. 저장소 사이에서 대화를 복사하지 않는다. */
public class SelectableMemoryService {
    private final Map<MemoryStorage, ChatMemory> memoriesByStorage;

    public SelectableMemoryService(ChatMemory inMemoryChatMemory, ChatMemory redisChatMemory) {
        memoriesByStorage = Map.of(
                MemoryStorage.IN_MEMORY, inMemoryChatMemory,
                MemoryStorage.REDIS, redisChatMemory);
    }

    public ChatMemory memoryFor(MemoryStorage storage) {
        return memoriesByStorage.get(Objects.requireNonNull(storage, "저장 위치를 지정하세요."));
    }
}
