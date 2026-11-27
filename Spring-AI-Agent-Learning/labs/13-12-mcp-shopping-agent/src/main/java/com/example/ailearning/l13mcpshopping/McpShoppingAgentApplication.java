package com.example.ailearning.l13mcpshopping;

import java.util.Set;
import org.springframework.ai.mcp.McpToolFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpShoppingAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpShoppingAgentApplication.class, args);
    }

    @Bean
    McpToolFilter counselToolFilter() {
        Set<String> counselToolNames = Set.of("searchProducts", "quoteCart", "getReturnPolicy");
        return (connection, tool) -> counselToolNames.contains(tool.name());
    }
}
