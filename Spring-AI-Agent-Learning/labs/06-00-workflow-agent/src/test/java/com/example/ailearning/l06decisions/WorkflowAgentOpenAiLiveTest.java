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
 * 실제 OpenAI를 호출한다. 저장소 루트 .env를 사용하며 API 사용량이 발생한다.
 * 검증 메서드 설명은 과정의 docs/lessons/06-workflow-agent/04-test-assertions.md를 참고한다.
 * @see <a href="../../../../../../../../../docs/lessons/06-workflow-agent/04-test-assertions.md">06장 보충: 테스트 검증 코드 읽기</a>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.ai.openai.chat.timeout=30s" //yaml의 5s 설정을 덮어 씀
})
class WorkflowAgentOpenAiLiveTest {
    private static final String SHOPPING_QUESTION =
            "음료 상품을 검색하고 실제 배송비를 확인해, 배송비 포함 30000원 이하 선물을 추천해줘.";

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired ComparisonService service;

    // 실제 카탈로그 동작을 유지하고 호출 순서·인자를 기록한다. 기록은 테스트마다 초기화된다.
    // Workflow는 직접 호출하며, Agent·혼합 방식은 요청별 도구 객체 안에서 호출한다.
    @MockitoSpyBean ProductCatalog productCatalog;

    @Test
    @Timeout(180)
    void 실제_OpenAI_Workflow는_검색과_가격_계산을_정해진_순서로_실행한다() {
        // When: 공통 질문을 Java가 순서를 정하는 Workflow로 실행한다.
        RunResult result = service.run(ExecutionMode.WORKFLOW, SHOPPING_QUESTION);

        // Then: 실제 호출 기록을 출력하고 고정 실행 순서와 추천 후보를 검증한다.
        System.out.printf("Workflow 결과: %s%n", result);
        ToolCallPrinter.printMockitoDetails(productCatalog);

        // 모델은 조건 추출·설명을 맡고, Java가 검색 → P01 계산 → P02 계산을 실행한다.
        // InOrder는 실행 순서를 지시하는 것이 아니라 이미 실행된 기록을 검증한다.
        var callOrder = inOrder(productCatalog);
        callOrder
                .verify(productCatalog)
                .search("음료", 30000);
        callOrder
                .verify(productCatalog)
                .calculatePriceDetails("P01");
        callOrder
                .verify(productCatalog)
                .calculatePriceDetails("P02");
        verifyNoMoreInteractions(productCatalog);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.answer()).isNotBlank();

        // P02는 상품가 28000원 + 배송비 3000원으로 총예산을 초과한다.
        // extracting으로 ID를 꺼내고, containsExactly로 P01 하나뿐인지 확인한다.
        assertThat(result.offers())
                .extracting(offer -> offer.product().id())
                .containsExactly("P01");
    }

    @Test
    @Timeout(180)
    void 실제_OpenAI_Agent가_선택한_도구의_조회_순서를_출력한다() {
        // When: 공통 질문의 도구 선택을 모델에 맡겨 실행한다.
        RunResult result = service.run(ExecutionMode.AGENT, SHOPPING_QUESTION);

        // Then: 모델이 선택한 실제 조회 기록을 출력하고 조회 결과를 확인한다.
        System.out.printf("Agent 결과: %s%n", result);
        ToolCallPrinter.printMockitoDetails(productCatalog);

        // 모델이 호출한 ShoppingTools의 도구는 내부에서 ProductCatalog를 호출한다.
        // ShoppingTools.search → ProductCatalog.search
        // ShoppingTools.calculatePriceDetails → ProductCatalog.calculatePriceDetails
        // ProductCatalog의 호출 기록으로 검색과 가격 조회가 모두 실행됐는지 검증한다.
        // 실제 모델의 도구 선택·반복 횟수는 달라질 수 있으므로 고정 순서를 검증하지 않는다.
        // atLeastOnce는 1회 이상을 뜻한다. any는 임의 인자, anyString은 null이 아닌 임의 문자열을 허용한다.
        verify(productCatalog, atLeastOnce()).search(any(), any());
        verify(productCatalog, atLeastOnce()).calculatePriceDetails(anyString());
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.answer()).isNotBlank();
        assertThat(result.offers()).isNotEmpty();
    }

    @Test
    @Timeout(180)
    void 실제_OpenAI_혼합_방식은_확정_조건과_조회한_금액으로_후보를_검증한다() {
        // When: 공통 질문을 조건 고정·모델 도구 선택·Java 검증의 혼합 방식으로 실행한다.
        RunResult result = service.run(ExecutionMode.HYBRID, SHOPPING_QUESTION);

        // Then: 실제 조회 기록을 출력하고 확정 조건과 최종 후보를 검증한다.
        System.out.printf("혼합 방식 결과: %s%n", result);
        ToolCallPrinter.printMockitoDetails(productCatalog);

        // 모델이 검색 인자를 선택해도 실제 검색은 Java가 확정한 조건으로 실행한다.
        // 도구의 선택 순서는 모델에 맡기고, 실제 조회와 최종 후보의 예산·카테고리를 검사한다.
        verify(productCatalog, atLeastOnce()).search("음료", 30000);
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.answer()).isNotBlank();

        // allSatisfy는 모든 추천에 아래 검사를 적용한다.
        // 빈 목록도 통과하므로 추천이 있는지 먼저 확인한다.
        assertThat(result.offers()).isNotEmpty();
        assertThat(result.offers())
                .allSatisfy(offer -> {
                    // 각 상품의 기존 가격 조회가 1회 이상 있었는지 검사한다. 다시 조회하지 않는다.
                    verify(productCatalog, atLeastOnce()).calculatePriceDetails(offer.product().id());
                    assertThat(offer.product().category()).isEqualTo("음료");
                    assertThat(offer.total()).isLessThanOrEqualTo(30000);
                });
    }
}
