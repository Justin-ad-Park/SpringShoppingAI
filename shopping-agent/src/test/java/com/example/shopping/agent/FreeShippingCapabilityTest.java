package com.example.shopping.agent;

import java.util.*;
import org.junit.jupiter.api.Test;
import com.example.shopping.agent.capabilities.*;
import com.example.shopping.agent.conversation.ConversationModels.*;
import static com.example.shopping.agent.ports.ShoppingContracts.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FreeShippingCapabilityTest {
    private final FreeShippingPolicy policy = new FreeShippingPolicy();
    @Test void policyRecalculatesDerivedBoundsButHonoursExplicitBudget() {
        assertThat(policy.bounds(42000,50000,Criteria.empty())).isEqualTo(new FreeShippingPolicy.Bounds(8000,15000));
        assertThat(policy.bounds(45000,50000,Criteria.empty())).isEqualTo(new FreeShippingPolicy.Bounds(5000,12000));
        assertThat(policy.bounds(45000,50000,new Criteria("",null,30000L))).isEqualTo(new FreeShippingPolicy.Bounds(5000,30000));
    }
    @Test void workflowPassesExplicitFiltersAndRejectsUnverifiedCandidates() {
        Catalog catalog = mock(Catalog.class);
        CartPricing cart = mock(CartPricing.class);
        Product existing = new Product("P001","그래놀라",18000,"식품","MALL",true,"🥣");
        Product valid = new Product("P004","허브티",9900,"음료","MALL",true,"🍵");
        Product wrongGroup = new Product("OTHER","다른배송",9000,"음료","PARTNER",true,"");
        when(cart.cart()).thenReturn(new Cart("standard",List.of(new CartItem(existing,1)),42000,"MALL"));
        when(cart.policy()).thenReturn(new Policy(50000,3000,"MALL","mock"));
        when(cart.quote(List.of())).thenReturn(new Quote(true,42000,3000,45000,""));
        when(cart.quote(List.of("P004"))).thenReturn(new Quote(true,51900,0,51900,""));
        when(catalog.search(any())).thenReturn(List.of(existing,wrongGroup,valid));
        var task = new SearchTask("task","cart.freeShippingRecommendation","standard",new Criteria("","음료",30000L));
        var result = new FreeShippingCapability(policy).execute(new Capability.Context("",task,null,catalog,cart,null));
        assertThat(result.products()).extracting(ChatDtos.Card::id).containsExactly("P004");
        var query = org.mockito.ArgumentCaptor.forClass(ProductQuery.class);
        verify(catalog).search(query.capture());
        assertThat(query.getValue().maxPrice()).isEqualTo(30000);
        assertThat(query.getValue().excludeProductIds()).containsExactly("P001");
        assertThat(query.getValue().category()).isEqualTo("음료");
        verify(cart,never()).quote(List.of("OTHER"));
    }
    @Test void catalogFallbackIsBoundedAndUsesTheSameValidatedQuery() {
        CatalogSearchStrategy strategy = mock(CatalogSearchStrategy.class);
        when(strategy.name()).thenReturn("AGENT");
        when(strategy.search(any(),any(),any())).thenThrow(new CatalogSearchStrategy.InvalidPlan());
        Catalog catalog = mock(Catalog.class);
        when(catalog.search(any())).thenReturn(List.of(new Product("P009","텀블러",29000,"생활","MALL",true,"🥤")));
        var task = new SearchTask("t","catalog.search","standard",new Criteria("텀블러",null,30000L));
        var result = new CatalogCapability(strategy).execute(new Capability.Context("",task,null,catalog,null,null));
        assertThat(result.execution()).isEqualTo("WORKFLOW_FALLBACK");
        assertThat(result.products().getFirst().shipping()).isNull();
        verify(catalog,times(1)).search(any());
    }
}
