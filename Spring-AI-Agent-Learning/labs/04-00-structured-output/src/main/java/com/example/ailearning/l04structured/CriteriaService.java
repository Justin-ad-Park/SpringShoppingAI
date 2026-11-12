package com.example.ailearning.l04structured;

import com.example.ailearning.l04structured.domain.SearchCriteria;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CriteriaService {
    static final String CRITERIA_EXTRACTION_INSTRUCTION = """
            사용자 요청에서 상품 카테고리(category)와 상품 가격 상한(maxPrice)을 추출하세요.
            category는 음료/생활 중 사용자가 명시한 것만 쓰세요.
            가격은 원 단위 정수입니다. 말하지 않은 필드는 null로 두세요.
            배송비는 계산하지 마세요. 실제 상품을 검색하는 작업도 하지 않습니다.
            """;
    private final ChatClient chatClient;

    public CriteriaService(ChatClient.Builder builder) {
        chatClient = builder.build();
    }

    public String extractCriteriaAsText(String question) {
        return chatClient
                .prompt()
                .system(CRITERIA_EXTRACTION_INSTRUCTION)
                .user(question)
                .call()
                .content();
    }

    public SearchCriteria extractCriteria(String question) {
        SearchCriteria criteria;
        try {
            // Spring AI가 형식 지시를 추가하고 응답을 record로 변환한다.
            criteria = chatClient
                    .prompt()
                    .system(CRITERIA_EXTRACTION_INSTRUCTION)
                    .user(question)
                    .call()
                    .entity(SearchCriteria.class);
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "모델 호출 또는 응답 변환에 실패했습니다.", error);
        }
        if (criteria == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "모델 응답이 비었습니다.");
        }
        try {
            // Java 객체로 바뀌었어도 업무 규칙은 별도로 검사한다.
            criteria.validate();
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, error.getMessage());
        }
        return criteria;
    }
}
