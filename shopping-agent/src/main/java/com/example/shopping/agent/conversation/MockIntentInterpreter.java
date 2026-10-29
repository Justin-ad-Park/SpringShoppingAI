package com.example.shopping.agent.conversation;

import java.util.*;
import java.util.regex.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import static com.example.shopping.agent.conversation.ConversationModels.*;

/** Limited, documented parser for offline demonstrations. Not a substitute for GPT language understanding. */
@Component
@ConditionalOnProperty(name="shopping.mode", havingValue="mock")
public class MockIntentInterpreter implements IntentInterpreter {
    private static final Pattern BUDGET = Pattern.compile("(-?[0-9][0-9,]*)\\s*(만)?\\s*원");
    public Decision interpret(String message, SearchTask active) {
        String text = message.replace("무료 배송", "무료배송");
        boolean fresh = text.contains("새로") || text.contains("새 검색") || text.contains("이전 조건");
        boolean noShipping = text.contains("상관없이") || text.contains("무료배송 조건은 빼") || text.contains("무료배송 빼");
        boolean shipping = text.contains("무료배송") && !noShipping;
        boolean coupons = text.contains("쿠폰"), points = text.contains("적립금");
        if (text.contains("전체") || text.contains("두 번째") || text.contains("그중") || text.contains("xxx"))
            return clarify("추가 상품의 예산·카테고리를 명시해 주세요. 전체 주문 예산이나 이전 결과 참조는 아직 지원하지 않습니다.");
        if ((coupons || points) && shipping) return clarify("혜택 조회와 무료배송 추천 중 먼저 처리할 업무를 알려 주세요.");
        if (coupons || points) return new Decision(Action.INDEPENDENT, "benefits.summary", null, null, null, coupons, points, null);
        if (shipping && (text.contains("조건") || text.contains("정책")) && !text.contains("추천") && !text.contains("상품"))
            return new Decision(Action.INDEPENDENT, "shipping.policy", null, null, null, false, false, null);
        String category = List.of("식품", "음료", "생활").stream().filter(text::contains).reduce((a,b) -> b).orElse(null);
        if (text.contains("카테고리") && category == null && !text.contains("해제") && !text.contains("없애"))
            return clarify("지원 카테고리는 식품·음료·생활입니다. 어느 카테고리에서 찾을까요?");
        String keyword = List.of("텀블러", "그래놀라", "오트", "견과", "허브티", "커피", "꿀", "프로틴", "잼")
            .stream().filter(text::contains).findFirst().orElse(null);
        Matcher matcher = BUDGET.matcher(text);
        Long budget = null;
        if (matcher.find()) {
            try { budget = Math.multiplyExact(Long.parseLong(matcher.group(1).replace(",", "")), matcher.group(2) == null ? 1 : 10000); }
            catch (ArithmeticException | NumberFormatException e) { return clarify("예산을 원 단위 정수로 알려 주세요."); }
        }
        boolean clearBudget = text.contains("예산") && (text.contains("해제") || text.contains("취소") || text.contains("없애"));
        boolean clearCategory = text.contains("카테고리") && (text.contains("해제") || text.contains("없애"));
        TextChange categoryChange = clearCategory ? new TextChange(Operation.CLEAR, null)
            : category == null ? null : new TextChange(Operation.SET, category);
        BudgetChange budgetChange = clearBudget ? new BudgetChange(Operation.CLEAR, null)
            : budget == null ? null : new BudgetChange(Operation.SET, budget);
        TextChange keywordChange = keyword == null ? null : new TextChange(Operation.SET, keyword);
        boolean conditionOnly = category != null || budgetChange != null || clearCategory;
        if (active == null && !fresh && !shipping && keyword == null && conditionOnly)
            return clarify("이전 검색이 없거나 만료됐습니다. 무엇을 검색할지 먼저 알려 주세요.");
        if (active != null && !fresh && !noShipping && (!shipping || active.capabilityId().equals("cart.freeShippingRecommendation"))
                && (conditionOnly || text.contains("만")))
            return new Decision(Action.REFINE, active.capabilityId(), keywordChange, categoryChange, budgetChange, false, false, null);
        if (active != null && !fresh && !shipping && !noShipping && keyword != null)
            return clarify("이전 검색 조건을 유지할까요? 새 검색이면 '새로'를 붙이거나 새 검색 버튼을 눌러 주세요.");
        if (shipping || keyword != null || noShipping || (fresh && conditionOnly))
            return new Decision(Action.NEW, shipping ? "cart.freeShippingRecommendation" : "catalog.search",
                keywordChange, categoryChange, budgetChange, false, false, null);
        return clarify("상품명 또는 무료배송 추천 요청을 알려 주세요. Mock은 식품·음료·생활 및 예산 변경 예시를 지원합니다.");
    }
    private Decision clarify(String question) { return new Decision(Action.CLARIFY, null, null, null, null, false, false, question); }
}
