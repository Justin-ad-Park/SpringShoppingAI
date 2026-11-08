package com.example.ailearning.l06decisions;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Service
public class ComparisonService {
    private static final Logger log = LoggerFactory.getLogger(ComparisonService.class);
    static final String EXTRACT_CATEGORY_BUDGET = """
            TASK:EXTRACT
            명시한 상품 카테고리(category: 음료/생활)와 배송비 포함 총예산(totalBudget)을 추출하세요.
            예산은 원 단위 정수입니다. 말하지 않은 필드는 null로 두세요.
            """;
    static final String AGENT_GOAL = """
            TASK:AGENT
            배송비 포함 예산에 맞는 선물을 추천하세요. 실제 가격은 제공된 도구 결과만 쓰세요.
            필요에 따라 상품 검색과 배송비 조회를 선택하고 결과에 따라 다음 행동을 결정하세요.
            필수 정보가 부족하면 질문하고, 추천할 수 없으면 이유를 설명하세요.
            일반 인사나 상품 이름만 묻는 요청은 불필요한 도구 없이 답할 수 있습니다.
            """;
    private final ChatClient chatClient;
    private final ProductCatalog productCatalog;

    public ComparisonService(ChatClient.Builder builder, ProductCatalog productCatalog) {
        this.chatClient = builder.build();
        this.productCatalog = productCatalog;
    }

    public RunResult run(String mode, String question) {
        try {
            ExecutionResult result = switch (mode) {
                case "workflow" -> workflow(question);
                case "agent" -> agent(question);
                case "hybrid" -> hybrid(question);
                default -> throw new IllegalArgumentException("mode는 workflow/agent/hybrid입니다.");
            };
            return new RunResult(mode, result.status(), result.answer(), result.offers());
        } catch (RuntimeException error) {
            Throwable causeType = error;
            while (causeType.getCause() != null) causeType = causeType.getCause();
            log.warn("Comparison failed: stage={}, errorType={}", mode, causeType.getClass().getSimpleName());
            return new RunResult(mode, "FAILED", "모델 연결과 응답 형식을 확인하세요.", List.of());
        }
    }

    private ProductSelectionCriteria extractProductSelectionCriteria(String question) {
        ProductSelectionCriteria criteria = chatClient
                .prompt()
                .system(EXTRACT_CATEGORY_BUDGET)
                .user(question)
                .call()
                .entity(ProductSelectionCriteria.class);
        if (criteria == null) throw new IllegalStateException("조건이 비었습니다.");
        criteria.validate();
        return criteria;
    }

    private ExecutionResult workflow(String question) {
        ProductSelectionCriteria criteria = extractProductSelectionCriteria(question);
        if (criteria.totalBudget() == null) return needsBudget();
        List<Product> products = productCatalog.search(criteria.category(), criteria.totalBudget());
        List<ProductPriceDetails> offers = products
                .stream()
                .map(product -> productCatalog.calculatePriceDetails(product.id()))
                .filter(productPriceDetails -> productPriceDetails.total() <= criteria.totalBudget())
                .toList();
        if (offers.isEmpty()) return new ExecutionResult("NO_RESULTS", "예산 안의 후보가 없습니다.", offers);
        String answer = chatClient
                .prompt()
                .system("TASK:SUMMARY\nJava가 검증한 후보만 설명하세요.")
                .user("질문: " + question + "\n확정 후보: " + offers)
                .call()
                .content();
        return new ExecutionResult("COMPLETED", answer, offers);
    }

    private ExecutionResult agent(String question) {
        ShoppingTools shoppingTools = new ShoppingTools(productCatalog);
        String answer = chatClient
                .prompt()
                .system(AGENT_GOAL)
                .user(question)
                .tools(shoppingTools)
                .call()
                .content();
        // 실제 조회한 금액이다. 모델의 추천 문장이나 예산 준수를 검증한 결과는 아니다.
        return new ExecutionResult("COMPLETED", answer, shoppingTools.getPriceDetails());
    }

    private ExecutionResult hybrid(String question) {
        ProductSelectionCriteria criteria = extractProductSelectionCriteria(question);
        if (criteria.totalBudget() == null) return needsBudget();
        BudgetShoppingTools shoppingTools = new BudgetShoppingTools(productCatalog, criteria);
        chatClient
                .prompt()
                .system(AGENT_GOAL)
                .user(question + "\n코드로 확정한 조건: " + criteria)
                .tools(shoppingTools)
                .call()
                .content();
        List<ProductPriceDetails> eligible = shoppingTools
                .getPriceDetails()
                .stream()
                .filter(productPriceDetails -> criteria.category() == null || productPriceDetails
                        .product()
                        .category()
                        .equals(criteria.category()))
                .filter(productPriceDetails -> productPriceDetails.total() <= criteria.totalBudget())
                .toList();
        // 최종 후보와 표시 문장은 코드로 확정한다. 미조회·예산 초과 상품은 포함하지 않는다.
        return new ExecutionResult(eligible.isEmpty() ? "NO_VERIFIED_OFFER" : "COMPLETED",
                eligible.isEmpty() ? "검증된 예산 안의 후보가 없습니다."
                        : "Java 검증 후보: " + eligible
                .stream()
                .map(productPriceDetails -> productPriceDetails
                        .product()
                        .id())
                .toList(), eligible);
    }

    private ExecutionResult needsBudget() { return new ExecutionResult("NEEDS_INPUT", "배송비 포함 예산을 알려주세요.", List.of()); }

    private record ExecutionResult(String status, String answer, List<ProductPriceDetails> offers) {}
}
