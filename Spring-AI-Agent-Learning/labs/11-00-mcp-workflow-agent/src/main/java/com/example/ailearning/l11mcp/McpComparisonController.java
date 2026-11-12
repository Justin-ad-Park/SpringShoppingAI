package com.example.ailearning.l11mcp;

import com.example.ailearning.l11mcp.tools.ShoppingMcpGateway;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
public class McpComparisonController {
    private final ShoppingMcpGateway shoppingMcpGateway;
    private final McpWorkflowService workflowService;
    private final McpAgentService agentService;
    private final McpHybridService hybridService;

    public McpComparisonController(ShoppingMcpGateway shoppingMcpGateway,
            McpWorkflowService workflowService, McpAgentService agentService, McpHybridService hybridService) {
        this.shoppingMcpGateway = shoppingMcpGateway;
        this.workflowService = workflowService;
        this.agentService = agentService;
        this.hybridService = hybridService;
    }

    @GetMapping("/tools")
    public List<String> tools() {
        return shoppingMcpGateway.listToolNames();
    }

    @GetMapping("/shipping-policy")
    public JsonNode shippingPolicy() {
        return shoppingMcpGateway.callTool("getShippingPolicy", Map.of());
    }

    @PostMapping(value = "/{mode:workflow|agent|hybrid}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public McpRunResult compare(@PathVariable String mode,
            @RequestBody @NotBlank @Size(max = 2000) String question) {
        return switch (mode) {
            case "workflow" -> workflowService.run(question);
            case "agent" -> agentService.run(question);
            case "hybrid" -> hybridService.run(question);
            default -> throw new IllegalArgumentException("지원하지 않는 실행 방식입니다.");
        };
    }
}
