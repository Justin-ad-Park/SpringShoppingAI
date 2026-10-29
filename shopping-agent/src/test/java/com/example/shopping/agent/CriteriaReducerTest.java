package com.example.shopping.agent;

import org.junit.jupiter.api.Test;
import com.example.shopping.agent.conversation.*;
import static com.example.shopping.agent.conversation.ConversationModels.*;
import static org.assertj.core.api.Assertions.*;

class CriteriaReducerTest {
    private final CriteriaReducer reducer = new CriteriaReducer();
    private Decision decision(Action action, TextChange category, BudgetChange budget) {
        return new Decision(action, "cart.freeShippingRecommendation", null, category, budget, false, false, null);
    }
    @Test void refineRetainsTaskAndUntouchedUserBudget() {
        SearchTask before = new SearchTask("id", "cart.freeShippingRecommendation", "standard", new Criteria("", "식품", 30000L));
        SearchTask after = reducer.apply(decision(Action.REFINE, new TextChange(Operation.SET,"음료"), null), before,"standard");
        assertThat(after.id()).isEqualTo("id");
        assertThat(after.constraints()).isEqualTo(new Criteria("", "음료", 30000L));
    }
    @Test void newTaskDoesNotInheritConditionsAndClearRestoresDefault() {
        SearchTask before = new SearchTask("id", "cart.freeShippingRecommendation", "standard", new Criteria("커피", "음료", 30000L));
        SearchTask fresh = reducer.apply(decision(Action.NEW,null,null),before,"standard");
        assertThat(fresh.id()).isNotEqualTo(before.id());
        assertThat(fresh.constraints()).isEqualTo(Criteria.empty());
        SearchTask cleared = reducer.apply(decision(Action.REFINE,new TextChange(Operation.CLEAR,null), new BudgetChange(Operation.CLEAR,null)),before,"standard");
        assertThat(cleared.constraints()).isEqualTo(new Criteria("커피",null,null));
    }
    @Test void validatesBudgetCategoryAndKeepsZeroDistinctFromMissing() {
        assertThat(reducer.apply(decision(Action.NEW,null,new BudgetChange(Operation.SET,0L)),null,"standard").constraints().maxPrice()).isZero();
        assertThatThrownBy(() -> reducer.apply(decision(Action.NEW,null,new BudgetChange(Operation.SET,-1L)),null,"standard")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reducer.apply(decision(Action.NEW,new TextChange(Operation.SET,"unknown"),null),null,"standard")).isInstanceOf(IllegalArgumentException.class);
    }
}
