package com.example.ailearning.l07runtime;

import com.example.ailearning.l07runtime.domain.ProductCatalog;
import com.example.ailearning.l07runtime.domain.ProductCatalog.Product;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {
    public record Reply(ChatIntent intent, MemoryStorage storage, String answer) {}
    private final IntentClassifier intentClassifier;
    private final MemoryChatService chatService;
    private final ProductCatalog productCatalog;

    public ConversationService(IntentClassifier intentClassifier, MemoryChatService chatService,
            ProductCatalog productCatalog) {
        this.intentClassifier = intentClassifier;
        this.chatService = chatService;
        this.productCatalog = productCatalog;
    }

    public Reply chat(String conversationId, String question) {
        ChatIntent intent = intentClassifier.classify(question);
        MemoryStorage storage = switch (intent) {
            case SMALL_TALK -> MemoryStorage.IN_MEMORY;
            case PRODUCT_SEARCH -> MemoryStorage.REDIS;
        };
        List<Product> products = intent == ChatIntent.PRODUCT_SEARCH ? productCatalog.findAll() : List.of();
        String answer = chatService.chat(storage, conversationId, question, products);
        return new Reply(intent, storage, answer);
    }
}
