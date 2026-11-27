package com.example.shopping.mcp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

@Configuration
public class ToolsConfiguration {
    @Bean
    ToolCallbackProvider shoppingToolCallbacks(CatalogTools catalog, CartTools cart,
            PricingTools pricing, BenefitsTools benefits) {
        return MethodToolCallbackProvider.builder().toolObjects(catalog, cart, pricing, benefits).build();
    }

    @Bean
    @ConditionalOnProperty(name = "shopping.mcp.return-policy.enabled", havingValue = "true")
    ToolCallbackProvider returnPolicyToolCallbacks() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(new ReturnPolicyTools())
                .build();
    }
}
