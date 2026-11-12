package com.example.ailearning.l06decisions;

import com.example.ailearning.l06decisions.domain.Product;
import com.example.ailearning.l06decisions.domain.ProductCatalog;
import com.example.ailearning.l06decisions.domain.ProductPriceDetails;
import com.example.ailearning.l06decisions.domain.ProductSelectionCriteria;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Service
public class ComparisonService {
    private static final Logger log = LoggerFactory.getLogger(ComparisonService.class);
    static final String PRODUCT_CRITERIA_EXTRACTION_INSTRUCTION = """
            TASK:EXTRACT
            명시한 상품 카테고리(category: 음료/생활)와 배송비 포함 총예산(totalBudget)을 추출하세요.
            예산은 원 단위 정수입니다. 말하지 않은 필드는 null로 두세요.
            """;
    static final String SHOPPING_AGENT_INSTRUCTION = """
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

    public RunResult run(ExecutionMode mode, String question) {
        try {
            ExecutionResult result = switch (mode) {
                case WORKFLOW -> runWorkflow(question);
                case AGENT -> runAgent(question);
                case HYBRID -> runHybrid(question);
            };
            return new RunResult(mode, result.status(), result.answer(), result.offers());
        } catch (RuntimeException error) {
            Throwable rootCause = error;
            while (rootCause.getCause() != null) rootCause = rootCause.getCause();
            log.warn("Comparison failed: stage={}, errorType={}", mode, rootCause.getClass().getSimpleName());
            return new RunResult(mode, "FAILED", "모델 연결과 응답 형식을 확인하세요.", List.of());
        }
    }

    private ProductSelectionCriteria extractProductSelectionCriteria(String question) {
        ProductSelectionCriteria criteria = chatClient
                .prompt()
                .system(PRODUCT_CRITERIA_EXTRACTION_INSTRUCTION)
                .user(question)
                .call()
                .entity(ProductSelectionCriteria.class);
        if (criteria == null) throw new IllegalStateException("조건이 비었습니다.");
        criteria.validate();
        return criteria;
    }

    private ExecutionResult runWorkflow(String question) {
        ProductSelectionCriteria criteria = extractProductSelectionCriteria(question);
        if (criteria.totalBudget() == null) return createBudgetQuestion();
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

    private ExecutionResult runAgent(String question) {
        ShoppingTools shoppingTools = new ShoppingTools(productCatalog);
        String answer = chatClient
                .prompt()
                .system(SHOPPING_AGENT_INSTRUCTION)
                .user(question)
                .tools(shoppingTools)
                .call()
                .content();
        // 실제 조회한 금액이다. 모델의 추천 문장이나 예산 준수를 검증한 결과는 아니다.
        return new ExecutionResult("COMPLETED", answer, shoppingTools.getPriceDetails());
    }

    private ExecutionResult runHybrid(String question) {
        ProductSelectionCriteria criteria = extractProductSelectionCriteria(question);
        if (criteria.totalBudget() == null) return createBudgetQuestion();
        BudgetShoppingTools shoppingTools = new BudgetShoppingTools(productCatalog, criteria);
        chatClient
                .prompt()
                .system(SHOPPING_AGENT_INSTRUCTION)
                .user(question + "\n코드로 확정한 조건: " + criteria)
                .tools(shoppingTools)
                .call()
                .content();
        List<ProductPriceDetails> verifiedOffers = shoppingTools
                .getPriceDetails()
                .stream()
                .filter(offer -> criteria.category() == null || offer.product().category().equals(criteria.category()))
                .filter(offer -> offer.total() <= criteria.totalBudget())
                .toList();
        if (verifiedOffers.isEmpty()) {
            return new ExecutionResult("NO_VERIFIED_OFFER", "검증된 예산 안의 후보가 없습니다.", verifiedOffers);
        }

        // 최종 후보와 표시 문장은 코드로 확정한다. 미조회·예산 초과 상품은 포함하지 않는다.
        List<String> productIds = verifiedOffers
                .stream()
                .map(offer -> offer.product().id())
                .toList();
        String answer = "Java 검증 후보: " + productIds;
        return new ExecutionResult("COMPLETED", answer, verifiedOffers);
    }

    private ExecutionResult createBudgetQuestion() { return new ExecutionResult("NEEDS_INPUT", "배송비 포함 예산을 알려주세요.", List.of()); }

    private record ExecutionResult(String status, String answer, List<ProductPriceDetails> offers) {}
}
