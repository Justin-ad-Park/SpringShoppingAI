package com.example.shopping.tutor.support;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

@Configuration
@ConditionalOnProperty(name="spring.ai.mcp.server.enabled",havingValue="true")
public class PolicyMcpConfiguration {
    public static class PolicyTools {
        @Tool(description="Tutor 가상 정책을 조회합니다. 실제 몰 규정이 아닙니다.")
        public Map<String,Object> getTutorPolicy() { return Map.of("policy","미개봉 상품은 수령 후 7일 이내 반품 가능","virtual",true,"version","v1"); }
    }
    @Bean ToolCallbackProvider policyCallbacks() { return MethodToolCallbackProvider.builder().toolObjects(new PolicyTools()).build(); }
}
