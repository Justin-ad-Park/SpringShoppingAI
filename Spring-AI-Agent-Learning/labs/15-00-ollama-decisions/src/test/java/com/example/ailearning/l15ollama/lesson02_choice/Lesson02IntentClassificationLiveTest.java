package com.example.ailearning.l15ollama.lesson02_choice;

import com.example.ailearning.l15ollama.domain.ShoppingIntent;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

/** 실제 로컬 Ollama와 다운로드한 의사결정 모델이 필요하다. OpenAI·Jev는 호출하지 않는다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Timeout(180)
class Lesson02IntentClassificationLiveTest {
    @Autowired Lesson02IntentClassificationService intentClassificationService;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 실제_Ollama가_상품_추천과_배송_문의의_의도를_분류한다() {
        var productResult = intentClassificationService.classifyIntent("3만원 이하 텀블러를 추천해 주세요.");
        var deliveryResult = intentClassificationService.classifyIntent("이미 주문한 상품이 아직 안 왔어요. 배송 위치가 궁금합니다.");
        System.out.println("상품 문의 결과: " + productResult);
        System.out.println("배송 문의 결과: " + deliveryResult);
        assertThat(productResult.predictedIntent()).isEqualTo(ShoppingIntent.PRODUCT_SEARCH);
        assertThat(deliveryResult.predictedIntent()).isEqualTo(ShoppingIntent.ORDER_DELIVERY);
        assertThat(productResult.probabilities()).containsKey(productResult.predictedIntent().name());
    }
}
