package com.example.ailearning.l05limits;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallLimitExceededException;
import org.springframework.stereotype.Service;

@Service
public class ToolLimitService {
    public record Reply(String status, String answer, String finishReason) {}
    private final ChatClient chatClient;
    private final ShoppingTools shoppingTools;

    public ToolLimitService(ChatClient.Builder builder, ShoppingTools shoppingTools) {
        this.chatClient = builder.build();
        this.shoppingTools = shoppingTools;
    }

    public Reply ask(String question) {
        var response = chatClient
                .prompt()
                .system("필요한 도구로 상품 가격과 배송비를 확인해 답하세요. 일반 인사에는 도구가 필요하지 않습니다.")
                .user(question)
                .tools(shoppingTools)
                .call()
                .chatResponse();
        String finishReason = response.getResult().getMetadata().getFinishReason();
        if (ToolCallLimitExceededException.FINISH_REASON.equals(finishReason)) {
            return new Reply("TOOL_LIMIT", "도구 호출 상한에 도달했습니다.", finishReason);
        }
        return new Reply("COMPLETED", response.getResult().getOutput().getText(), finishReason);
    }
}
