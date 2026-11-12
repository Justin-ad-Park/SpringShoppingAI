package com.example.ailearning.l06decisions;

import com.example.ailearning.l06decisions.domain.ProductCatalog;
import com.example.ailearning.testsupport.TestNamePrinter;
import com.example.ailearning.testsupport.ToolCallPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 실제 OpenAI를 호출하며 API 사용량이 발생합니다. 저장소 루트 .env를 사용합니다.
 * 검증 코드 설명: docs/lessons/06-workflow-agent/04-test-assertions.md
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.ai.openai.timeout=30s"
})
@Timeout(180)
class WorkflowAgentOpenAiLiveTest {
    private static final String SHOPPING_QUESTION =
            "친구에게 줄 음료 선물을 찾고 있어요. 배송비까지 3만 원 안에서 추천해 주세요.";

    @Autowired ShoppingService service;
    // 실제 검색·계산을 실행하면서 호출 기록만 남깁니다. 기록은 테스트마다 초기화됩니다.
    @MockitoSpyBean ProductCatalog productCatalog;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void Workflow는_조건을_추출한_뒤_정해진_순서로_상품과_배송비를_조회한다() {
        RunResult result = run(ExecutionMode.WORKFLOW, SHOPPING_QUESTION);

        var order = inOrder(productCatalog);
        order.verify(productCatalog).search("음료", 30000);
        order.verify(productCatalog).calculatePriceDetails("P01");
        order.verify(productCatalog).calculatePriceDetails("P02");
        verifyNoMoreInteractions(productCatalog);
        assertCompleted(result);
    }

    @Test
    void Agent는_예산에_맞는_추천을_위해_검색과_배송비_조회를_선택한다() {
        RunResult result = run(ExecutionMode.AGENT, SHOPPING_QUESTION);

        // 실제 모델은 재검색하거나 여러 상품을 비교할 수 있어 순서·횟수를 고정하지 않습니다.
        verify(productCatalog, atLeastOnce()).search(any(), any());
        verify(productCatalog, atLeastOnce()).calculatePriceDetails(anyString());
        assertCompleted(result);
    }

    @Test
    void Hybrid는_확정한_조건으로_예산_내_상품_조회_도구를_실행한다() {
        RunResult result = run(ExecutionMode.HYBRID, SHOPPING_QUESTION);

        verify(productCatalog, atLeastOnce()).search("음료", 30000);
        verify(productCatalog, atLeastOnce()).calculatePriceDetails("P01");
        verify(productCatalog, atLeastOnce()).calculatePriceDetails("P02");
        assertCompleted(result);

        // 위 verify는 상품·금액을 조회했는지만 확인합니다.
        // 예산 초과 상품의 제외는 단위 테스트에서,
        // 제외한 결과가 모델에 전달되는지는 로컬 통합 테스트에서 확인합니다.
    }

    @Test
    void Workflow는_예산을_말하지_않으면_상품_조회_전에_되묻는다() {
        RunResult result = run(ExecutionMode.WORKFLOW, "친구에게 줄 음료 선물을 추천해 주세요.");

        assertThat(result.status()).isEqualTo("NEEDS_INPUT");
        assertThat(result.answer()).isNotBlank();
        verifyNoInteractions(productCatalog);
    }

    @Test
    void Agent는_상품_이름만_묻는_요청에_배송비_조회가_필요하지_않다() {
        RunResult result = run(ExecutionMode.AGENT, "어떤 음료 상품들이 있나요? 이름만 알려 주세요.");

        verify(productCatalog, atLeastOnce()).search(eq("음료"), any());
        verify(productCatalog, never()).calculatePriceDetails(anyString());
        assertCompleted(result);
    }

    @Test
    void Agent는_인사에_상품_조회_없이_응답한다() {
        RunResult result = run(ExecutionMode.AGENT, "안녕하세요! 어떤 도움을 받을 수 있나요?");

        verifyNoInteractions(productCatalog);
        assertCompleted(result);
    }

    @Test
    void Hybrid는_예산에_맞는_상품이_없어도_조회_결과를_설명한다() {
        RunResult result = run(ExecutionMode.HYBRID, "음료 선물을 추천해 주세요. 배송비까지 천 원 이하면 좋겠어요.");

        verify(productCatalog, atLeastOnce()).search("음료", 1000);
        verify(productCatalog, never()).calculatePriceDetails(anyString());
        assertCompleted(result);
    }

    private RunResult run(ExecutionMode mode, String question) {
        RunResult result = service.run(mode, question);
        System.out.printf("요청 [%s]: %s%n결과: %s%n", mode, question, result);
        ToolCallPrinter.printMockitoDetails(productCatalog);
        return result;
    }

    private void assertCompleted(RunResult result) {
        // COMPLETED는 실행 완료입니다. 자연어 답변 전체의 사실성을 보증하는 상태가 아닙니다.
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.answer()).isNotBlank();
    }
}
