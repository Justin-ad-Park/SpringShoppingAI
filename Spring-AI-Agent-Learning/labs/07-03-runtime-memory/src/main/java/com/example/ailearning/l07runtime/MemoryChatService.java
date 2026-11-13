package com.example.ailearning.l07runtime;

import com.example.ailearning.l07runtime.domain.ProductCatalog.Product;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
public class MemoryChatService {
    private final ChatClient chatClient;
    private final ChatMemorySelector memorySelector;

    public MemoryChatService(ChatClient.Builder builder, ChatMemorySelector memorySelector) {
        this.chatClient = builder.build();
        this.memorySelector = memorySelector;
    }

    // storage는 요청마다 달라집니다. 선택한 한 메모리에서 읽고 그 메모리에만 저장합니다.
    public synchronized String chat(MemoryStorage storage, String conversationId, String question,
            List<Product> products) {
        ChatMemory selectedMemory = memorySelector.select(storage);
        String answer = chatClient
                .prompt()
                .system("이전 대화와 현재 질문에 답하세요. 상품 정보는 제공된 조회 자료만 사용하세요. "
                        + "가격은 상품가이며 배송비·재고는 조회하지 않았습니다.\n상품 조회 자료: " + products)
                .messages(selectedMemory.get(conversationId))
                .user(question)
                .call()
                .content();
        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("빈 모델 응답은 저장하지 않습니다.");
        }
        selectedMemory.add(conversationId, List.of(new UserMessage(question), new AssistantMessage(answer)));
        return answer;
    }
}
