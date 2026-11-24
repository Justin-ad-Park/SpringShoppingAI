package com.example.ailearning.l15ollama.lesson06_fields;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

/** 실제 로컬 Ollama와 다운로드한 의사결정 모델이 필요하다. OpenAI·Jev는 호출하지 않는다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Timeout(180)
class Lesson06OrderContextLiveTest {
    @Autowired Lesson06OrderContextDecisionService orderContextDecisionService;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 실제_Ollama가_주문_상태와_고객_메시지를_함께_판단한다() {
        var inquiry = new Lesson06OrderContextDecisionService.OrderInquiry("배송된 상품을 반품하고 싶어요.", "DELIVERED", 2);
        var result = orderContextDecisionService.decideNextAction(inquiry);
        System.out.println("주문 정보 입력: " + inquiry);
        System.out.println("다음 안내 판단: " + result);
        assertThat(result.nextAction()).isEqualTo("EXPLAIN_RETURN");
        assertThat(result.probabilities()).containsKey(result.nextAction());
    }
}
