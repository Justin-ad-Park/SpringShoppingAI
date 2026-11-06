package com.example.ailearning.l06limits;

import com.example.ailearning.testsupport.TestNamePrinter;
import com.example.ailearning.testsupport.ToolCallPrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** 실제 OpenAI를 호출한다. 루트 .env를 사용하며 API 사용량이 발생한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ToolLimitOpenAiLiveTest {
    @Autowired ToolLimitService toolLimitService;
    @MockitoSpyBean ShoppingTools shoppingTools;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    @Timeout(180)
    void 실제_OpenAI가_허용_횟수_안에서_상품과_배송비를_조회한다() {
        // Given: 검색과 배송비 조회로 처리할 수 있는 정상 질문
        String question = "간식,음료 카테고리를 각각 검색하고 P01 허브티 가격과 배송비, 총액을 알려줘.";

        // When: 모델이 필요한 도구를 자유롭게 선택
        var reply = toolLimitService.ask(question);
        System.out.println("정상 결과: " + reply);
        ToolCallPrinter.printToolCalls(shoppingTools);

        // Then: 제한에 걸리지 않고 요청한 도구를 실제로 실행해 답변
        assertThat(reply.status()).isEqualTo("COMPLETED");
        assertThat(reply.answer()).isNotBlank();
        verify(shoppingTools, atLeastOnce()).searchProducts("음료");
        verify(shoppingTools, atLeastOnce()).calculateShipping("P01");
    }

    @Test
    @Timeout(180)
    void 실제_OpenAI가_세_카테고리를_검색하면_세번째_도구_실행을_차단한다() {
        // Given: 카테고리마다 같은 검색 도구를 한 번씩 사용할 요청
        String question = """
                음료, 생활, 간식 세 카테고리를 각각 검색해서 결과를 하나로 정리해줘.
                검색 도구는 한 번에 카테고리 하나씩 호출해줘. 배송비 조회는 필요 없어.
                """;

        // When: 모델이 질문을 처리하기 위해 검색 도구를 선택
        var reply = toolLimitService.ask(question);
        System.out.println("호출 제한 결과: " + reply);
        ToolCallPrinter.printToolCalls(shoppingTools);

        // Then: 검색은 2회 실행하고, 세 번째 요청은 실행 전에 차단
        assertThat(reply.status()).isEqualTo("TOOL_LIMIT");
        assertThat(reply.finishReason()).isEqualTo("toolCallLimitExceeded");
        verify(shoppingTools, times(2)).searchProducts(anyString());
        verify(shoppingTools, never()).calculateShipping(anyString());
    }
}
