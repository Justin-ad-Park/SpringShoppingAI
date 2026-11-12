package com.example.ailearning.l11mcp;

import com.example.ailearning.l11mcp.domain.SearchConditions;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class SearchConditionExtractor {
    private final ChatClient chatClient;

    public SearchConditionExtractor(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public SearchConditions extract(String question) {
        SearchConditions searchConditions = chatClient
                .prompt()
                .system("""
                        TASK:MCP_EXTRACT
                        category(식품/음료/생활), maxPrice(상품 하나의 가격 상한),
                        totalBudget(배송비 포함 장바구니 총예산)을 추출하세요.
                        가격과 예산의 단위는 원입니다. 말하지 않은 값은 null입니다.
                        """)
                .user(question)
                .call()
                .entity(SearchConditions.class);
        if (searchConditions == null) {
            throw new IllegalStateException("검색 조건이 비었습니다.");
        }
        searchConditions.validate();
        return searchConditions;
    }
}
