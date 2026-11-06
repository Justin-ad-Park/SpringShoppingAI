package com.example.ailearning.l06limits;

import com.example.ailearning.testsupport.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.mockito.Mockito.*;

/** 실제 OpenAI를 호출한다. 루트 .env를 사용하며 API 사용량이 발생한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ToolLimitOpenAiLiveTest {
    @Autowired ToolLimitService toolLimitService;
    @MockitoSpyBean ShoppingTools shoppingTools;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) { TestNamePrinter.printTestName(testInfo); }

    @Test
    @Timeout(180)
    void 실제_OpenAI로_상품_가격과_배송비를_조회한다() {
        var reply = toolLimitService.ask("음료를 검색하고 P01 허브티 가격과 배송비, 총액을 알려줘.");
        System.out.println("실제 모델 결과: " + reply);
        assertThat(reply.status()).isEqualTo("COMPLETED");
        assertThat(reply.answer()).isNotBlank();
        verify(shoppingTools, atLeastOnce()).searchProducts("음료");
        verify(shoppingTools, atLeastOnce()).calculateShipping("P01");
    }
}
