package com.example.shopping.agent;

import org.junit.jupiter.api.Test;
import com.example.shopping.agent.conversation.*;
import static com.example.shopping.agent.conversation.ConversationModels.*;
import static org.assertj.core.api.Assertions.*;

class MockIntentInterpreterTest {
    private final MockIntentInterpreter interpreter = new MockIntentInterpreter();
    private final SearchTask active = new SearchTask("id","cart.freeShippingRecommendation","standard",new Criteria("",null,30000L));
    @Test void supportsRefineIndependentLookupAndExplicitNewSearch() {
        var refine = interpreter.interpret("음료 카테고리에서만 추천해줘",active);
        assertThat(refine.action()).isEqualTo(Action.REFINE);
        assertThat(refine.category().value()).isEqualTo("음료");
        assertThat(refine.budget()).isNull();
        assertThat(interpreter.interpret("적립금 조회",active).action()).isEqualTo(Action.INDEPENDENT);
        assertThat(interpreter.interpret("새로 3만원 이하 텀블러 찾아줘",active).action()).isEqualTo(Action.NEW);
        assertThat(interpreter.interpret("예산 3만원",active).budget().value()).isEqualTo(30000L);
    }
    @Test void asksWhenThereIsNoPriorSearchOrTheRequestIsAmbiguous() {
        assertThat(interpreter.interpret("음료만",null).action()).isEqualTo(Action.CLARIFY);
        assertThat(interpreter.interpret("텀블러 찾아줘",active).action()).isEqualTo(Action.CLARIFY);
        assertThat(interpreter.interpret("무료배송 상관없이 텀블러 찾아줘",active).capabilityId()).isEqualTo("catalog.search");
    }
}
