package com.example.ailearning.l13shopping.lesson10_shoppingandreturn;

import com.example.ailearning.l13shopping.domain.ProductCatalog;
import com.example.ailearning.l13shopping.domain.ProductPriceDetail;
import com.example.ailearning.l13shopping.policy.PolicySearch;
import com.example.ailearning.l13shopping.policy.PolicySearchResult;
import java.util.Comparator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** 한 질문에 담긴 상품 추천과 반품 안내를 고정된 두 단계로 처리한다. */
@Service
public class Lesson10ShoppingAndReturnService {
    private static final String EXTRACT_SHOPPING_AND_RETURN_INPUTS = """
            고객이 요청한 상품 추천과 반품 안내에 필요한 입력을 각각 추출하세요.
            상품 추천에는 카테고리(category: 음료 또는 생활)와 배송비 포함 예산(totalBudget: 원)이 필요합니다.
            예산에 배송비 언급이 없으면 문의한 금액을 배송비 포함 예산으로 사용하세요.
            반품 안내에는 상품 개봉 상태(returnCondition: SEALED는 미개봉, OPENED는 개봉)가 필요합니다.
            요청이 없거나 알 수 없는 값은 해당 필드에 null을 넣으세요.
            """;

    private final ChatClient chatClient;
    private final ProductCatalog productCatalog;
    private final PolicySearch policySearch;

    public Lesson10ShoppingAndReturnService(ChatClient.Builder builder,
            ProductCatalog productCatalog, PolicySearch policySearch) {
        this.chatClient = builder.build();
        this.productCatalog = productCatalog;
        this.policySearch = policySearch;
    }

    public Result answer(String customerQuestion) {
        var request = chatClient
                .prompt()
                .system(EXTRACT_SHOPPING_AND_RETURN_INPUTS)
                .user(customerQuestion)
                .call()
                .entity(Lesson10ShoppingAndReturnRequest.class);

        if (request == null || !request.hasProductCriteria()) {
            return new Result("NEEDS_INPUT", "추천받을 상품 종류와 배송비 포함 예산을 알려주세요.",
                    null, null);
        }
        if (request.returnCondition() == null) {
            return new Result("NEEDS_INPUT", "반품할 상품을 개봉했는지 알려주세요.", null, null);
        }

        ProductPriceDetail recommendation = recommendProductWithinBudget(request);
        PolicySearchResult returnPolicy = findMatchingReturnPolicy(request.returnCondition());
        String productAnswer = recommendation == null
                ? "예산 안에서 추천할 상품을 찾지 못했습니다."
                : "추천 상품은 %s입니다. 상품가 %d원, 배송비 %d원, 총 %d원입니다.".formatted(
                        recommendation.product().name(), recommendation.product().price(),
                        recommendation.shippingFee(), recommendation.total());
        String returnAnswer = returnPolicy == null
                ? "상품의 개봉 상태에 해당하는 반품 정책을 확인하지 못했습니다."
                : returnPolicy.text();

        String status = recommendation != null && returnPolicy != null ? "COMPLETE" : "MISSING_RESULTS";
        return new Result(status, productAnswer + "\n" + returnAnswer, recommendation, returnPolicy);
    }

    private ProductPriceDetail recommendProductWithinBudget(Lesson10ShoppingAndReturnRequest request) {
        return productCatalog
                .search(request.category(), request.totalBudget())
                .stream()
                .map(product -> productCatalog.calculatePriceDetail(product.id()))
                .filter(priceDetail -> priceDetail.total() <= request.totalBudget())
                .min(Comparator.comparingInt(ProductPriceDetail::total))
                .orElse(null);
    }

    private PolicySearchResult findMatchingReturnPolicy(Lesson10ShoppingAndReturnRequest.ReturnCondition returnCondition) {
        return policySearch
                .searchPolicies(returnCondition.searchQuery())
                .stream()
                .filter(policy -> returnCondition.sourceId().equals(policy.id()))
                .findFirst()
                .orElse(null);
    }

    public record Result(String status, String answer, ProductPriceDetail recommendation, PolicySearchResult returnPolicy) {}
}
