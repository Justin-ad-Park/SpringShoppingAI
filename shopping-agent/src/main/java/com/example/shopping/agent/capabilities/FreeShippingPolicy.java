package com.example.shopping.agent.capabilities;

import org.springframework.stereotype.Component;
import com.example.shopping.agent.conversation.ConversationModels.Criteria;

/** Recommendation policy, deliberately separate from the shop's actual shipping rules. */
@Component
public class FreeShippingPolicy {
    public record Bounds(long minPrice, long maxPrice) {}
    public Bounds bounds(long subtotal, long threshold, Criteria criteria) {
        long remaining = Math.max(0, Math.subtractExact(threshold, subtotal));
        return new Bounds(remaining, criteria.maxPrice() == null ? Math.addExact(remaining, 7000) : criteria.maxPrice());
    }
}
