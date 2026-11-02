package com.example.shopping.agent;

import tools.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import com.example.shopping.agent.capabilities.*;

@Configuration
public class ExecutionConfiguration {
    @Bean
    CatalogSearchStrategy catalogSearchStrategy(@Value("${shopping.mode}") String mode,
            @Value("${shopping.catalog-execution:agent}") String execution,
            ObjectProvider<ChatClient.Builder> builders, ObjectMapper mapper) {
        if (!execution.equals("agent") && !execution.equals("workflow")) throw new IllegalArgumentException("catalog-execution은 agent/workflow입니다.");
        return mode.equals("gpt") && execution.equals("agent")
            ? new LlmCatalogSearch(builders.getObject().build(), mapper) : new DirectCatalogSearch();
    }
}
