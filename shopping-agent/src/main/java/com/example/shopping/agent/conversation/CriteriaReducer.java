package com.example.shopping.agent.conversation;

import java.util.*;
import org.springframework.stereotype.Component;
import static com.example.shopping.agent.conversation.ConversationModels.*;

/** Pure, deterministic SET/CLEAR/KEEP rules. No MCP or model calls. */
@Component
public class CriteriaReducer {
    public SearchTask apply(Decision decision, SearchTask active, String scenario) {
        boolean fresh = decision.action() == Action.NEW;
        if (!fresh && decision.action() != Action.REFINE) throw new IllegalArgumentException("검색 변경이 아닙니다.");
        if (!fresh && active == null) throw new IllegalArgumentException("이전 검색이 없습니다.");
        String capability = fresh ? decision.capabilityId() : active.capabilityId();
        if (!fresh && decision.capabilityId() != null && !decision.capabilityId().equals(capability))
            throw new IllegalArgumentException("업무 변경은 새 검색으로 요청하세요.");
        Criteria before = fresh ? Criteria.empty() : active.constraints();
        String keyword = text(before.keyword(), decision.keyword(), "");
        String category = text(before.category(), decision.category(), null);
        if (category != null && !List.of("식품", "음료", "생활").contains(category))
            throw new IllegalArgumentException("카테고리는 식품·음료·생활 중 선택해 주세요.");
        Long budget = before.maxPrice();
        BudgetChange change = decision.budget();
        if (change != null && change.operation() != null) {
            budget = switch (change.operation()) {
                case KEEP -> budget;
                case CLEAR -> null;
                case SET -> {
                    if (change.value() == null || change.value() < 0) throw new IllegalArgumentException("예산은 0원 이상이어야 합니다.");
                    yield change.value();
                }
            };
        }
        return new SearchTask(fresh ? UUID.randomUUID().toString() : active.id(), capability, scenario,
            new Criteria(keyword, category, budget));
    }
    private String text(String previous, TextChange change, String cleared) {
        if (change == null || change.operation() == null || change.operation() == Operation.KEEP) return previous;
        if (change.operation() == Operation.CLEAR) return cleared;
        if (change.value() == null || change.value().isBlank()) throw new IllegalArgumentException("변경할 값을 알려 주세요.");
        return change.value().strip();
    }
}
