package com.example.ailearning.l06decisions;

import com.example.ailearning.l06decisions.domain.Product;
import com.example.ailearning.l06decisions.domain.ProductCatalog;
import com.example.ailearning.l06decisions.domain.ProductPriceDetail;
import com.example.ailearning.l06decisions.domain.ProductSearchCondition;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Service
public class ShoppingService {
    private static final Logger log = LoggerFactory.getLogger(ShoppingService.class);
    static final String PRODUCT_SEARCH_CONDITION_EXTRACTION_INSTRUCTION = """
            TASK:EXTRACT
            명시한 상품 카테고리(category: 음료/생활)와 배송비 포함 총예산(totalBudget)을 추출하세요.
            예산은 원 단위 정수입니다. 말하지 않은 필드는 null로 두세요.
            """;
    static final String SHOPPING_AGENT_INSTRUCTION = """
            TASK:AGENT
            배송비 포함 예산에 맞는 선물을 추천하세요. 실제 가격은 제공된 도구 결과만 쓰세요.
            필요에 따라 상품 검색과 배송비 조회를 선택하고 결과에 따라 다음 행동을 결정하세요.
            필수 정보가 부족하면 질문하고, 추천할 수 없으면 이유를 설명하세요.
            일반 인사는 도구 없이 답하고, 상품 이름만 묻는 요청은 검색만으로 답할 수 있습니다.
            """;
    private final ChatClient chatClient;
    private final ProductCatalog productCatalog;

    public ShoppingService(ChatClient.Builder builder, ProductCatalog productCatalog) {
        this.chatClient = builder.build();
        this.productCatalog = productCatalog;
    }

    public RunResult run(ExecutionMode mode, String question) {
        try {
            ExecutionResult result = switch (mode) {
                case WORKFLOW -> runWorkflow(question);
                case AGENT -> runAgent(question);
                case HYBRID -> runHybrid(question);
            };
            return new RunResult(mode, result.status(), result.answer());
        } catch (RuntimeException error) {
            Throwable rootCause = error;
            while (rootCause.getCause() != null) rootCause = rootCause.getCause();
            log.warn("Shopping request failed: stage={}, errorType={}", mode, rootCause.getClass().getSimpleName());
            return new RunResult(mode, "FAILED", "모델 연결과 응답 형식을 확인하세요.");
        }
    }

    private ProductSearchCondition extractProductSearchCondition(String question) {
        ProductSearchCondition searchCondition = chatClient
                .prompt()
                .system(PRODUCT_SEARCH_CONDITION_EXTRACTION_INSTRUCTION)
                .user(question)
                .call()
                .entity(ProductSearchCondition.class);
        if (searchCondition == null) throw new IllegalStateException("조건이 비었습니다.");
        searchCondition.validate();
        return searchCondition;
    }

    private ExecutionResult runWorkflow(String question) {
        ProductSearchCondition searchCondition = extractProductSearchCondition(question);
        if (searchCondition.totalBudget() == null) return createBudgetQuestion();
        List<Product> products = productCatalog.search(searchCondition.category(), searchCondition.totalBudget());
        List<ProductPriceDetail> candidates = products
                .stream()
                .map(product -> productCatalog.calculatePriceDetails(product.id()))
                .filter(priceDetail -> priceDetail.total() <= searchCondition.totalBudget())
                .toList();
        if (candidates.isEmpty()) return new ExecutionResult("NO_RESULTS", "예산 안의 후보가 없습니다.");
        String answer = chatClient
                .prompt()
                .system("TASK:SUMMARY\nJava가 검증한 후보만 설명하세요.")
                .user("질문: " + question + "\n확정 후보: " + candidates)
                .call()
                .content();
        return new ExecutionResult("COMPLETED", answer);
    }

    private ExecutionResult runAgent(String question) {
        String answer = chatClient
                .prompt()
                .system(SHOPPING_AGENT_INSTRUCTION)
                .user(question)
                .tools(new ProductTools(productCatalog))
                .call()
                .content();
        return new ExecutionResult("COMPLETED", answer);
    }

    private ExecutionResult runHybrid(String question) {
        ProductSearchCondition searchCondition = extractProductSearchCondition(question);
        if (searchCondition.totalBudget() == null) return createBudgetQuestion();
        BudgetProductTools tools = new BudgetProductTools(productCatalog, searchCondition);
        String answer = chatClient
                .prompt()
                .system("""
                        TASK:HYBRID
                        배송비 포함 예산에 맞는 선물을 추천하세요.
                        상품 정보와 금액은 제공된 조회 도구 결과만 사용하세요.
                        도구는 Java가 확정한 카테고리와 예산에 맞는 상품만 반환합니다.
                        조회 결과가 비면 조건에 맞는 상품이 없다고 설명하세요.
                        """)
                .user(question)
                .tools(tools)
                .call()
                .content();
        // Java가 보장하는 범위는 도구가 반환하는 후보 조건입니다. 최종 자연어의 정확성을 검증한 것은 아닙니다.
        return new ExecutionResult("COMPLETED", answer);
    }

    private ExecutionResult createBudgetQuestion() {
        return new ExecutionResult("NEEDS_INPUT", "배송비 포함 예산을 알려주세요.");
    }

    private record ExecutionResult(String status, String answer) {}
}
