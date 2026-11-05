package com.example.ailearning.l05tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

// 실제 OpenAI를 호출합니다. 저장소 루트 .env를 사용하며 API 사용량이 발생합니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.ai.openai.base-url=https://api.openai.com/v1",
        "spring.ai.openai.chat.timeout=30s"
})
class ToolCallingOpenAiLiveTest {
    @Autowired ToolChatService service;

    // Spy는 실제 도구 동작을 유지하면서 호출 기록을 남긴다.
    @MockitoSpyBean ShoppingTools shoppingTools;

    @Test
    @Timeout(180)
    void 실제_OpenAI는_인사에_도구를_호출하지_않고_답한다(TestInfo testInfo) {
        String answer = service.ask("안녕하세요! 반갑습니다.");

        System.out.printf("[%s] 답변: %s%n", testInfo.getDisplayName(), answer);
        assertThat(answer).isNotBlank();

        // 답변 내용과 별도로 실제 도구 호출 기록이 하나도 없는지 검사한다.
        verifyNoInteractions(shoppingTools);
        System.out.printf("[%s] 확인한 ShoppingTools 호출 횟수: 0%n", testInfo.getDisplayName());
    }

    @Test
    @Timeout(180)
    void 실제_OpenAI에_가격과_배송비를_질문한다(TestInfo info) {
        String answer = service.ask("P02 커피의 가격과 배송비 포함 총액을 도구로 확인해줘.");
        System.out.printf("[%s] 답변: %s%n", info
                        .getTestMethod()
                        .orElseThrow()
                        .getName(), answer);
        assertThat(answer).isNotBlank();
    }

    @Test
    @Timeout(180)
    void 실제_OpenAI가_상품_검색_결과로_배송비를_조회한다(TestInfo testInfo) {
        // Given: 검색과 배송비 조회가 필요한 사용자 질문 준비
        String question = """
                상품 검색 도구로 음료 카테고리에서 상품 가격이 15000원 이하인 상품을 찾아줘.
                상품명, 상품 가격, 배송비, 배송비 포함 총액을 알려줘.
                """;

        // When: 실제 모델이 도구를 선택하도록 질문 전달
        String answer = service.ask(question);

        // Then: 답변과 Spy에 남은 실제 도구 호출 순서·인자 검증
        System.out.printf("[%s] 답변: %s%n", testInfo.getDisplayName(), answer);
        assertThat(answer).isNotBlank();

        // service.ask() 실행 중 남은 호출 기록을 검사한다. verify()는 도구를 다시 실행하지 않는다.
        // InOrder는 실행 순서를 지정하는 것이 아니라, 이미 실행된 순서를 검증한다.
        // P01은 현재 상품 데이터로 예상한 값이며, 위 verify()의 검색 결과에서 얻은 값이 아니다.
        InOrder toolCallOrder = inOrder(shoppingTools);
        toolCallOrder
                .verify(shoppingTools)
                .searchProducts("음료", 15000);
        toolCallOrder
                .verify(shoppingTools)
                .calculateShipping("P01");
        System.out.printf("[%s] 확인한 도구 호출: searchProducts(음료, 15000) → calculateShipping(P01)%n",
                testInfo.getDisplayName());
    }
}
