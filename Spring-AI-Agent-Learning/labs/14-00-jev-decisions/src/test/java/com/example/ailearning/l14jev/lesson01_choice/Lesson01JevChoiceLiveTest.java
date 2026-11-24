package com.example.ailearning.l14jev.lesson01_choice;

import com.example.ailearning.l14jev.domain.ShoppingIntent;
import com.example.ailearning.l14jev.lesson01_choice.Lesson01IntentClassificationService.IntentClassificationResult;
import java.util.List;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Jev API 호출: 루트 .env의 TYPESAFE_API_KEY가 필요하며 사용량이 발생합니다.
 * 이 클래스는 두 문장을 각각 판단하도록 두 번 요청합니다. OpenAI는 호출하지 않습니다.
 * 고정 응답 테스트와 달리 실제 모델의 판단이 기대와 다르면 실패할 수 있습니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class Lesson01JevChoiceLiveTest {
    @Autowired Lesson01IntentClassificationService intentService;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void Choice로_상품_추천과_주문_배송_문의의_의도를_구분한다() {
        String productMessage = "3만원 이하의 가벼운 텀블러를 추천해 주세요.";
        IntentClassificationResult productIntentResult = intentService.classifyIntent(productMessage);
        printJevDecision(productMessage, productIntentResult);
        String deliveryMessage = "이미 주문한 상품이 아직 도착하지 않았어요. 지금 배송 상태가 궁금합니다.";
        IntentClassificationResult deliveryIntentResult = intentService.classifyIntent(deliveryMessage);
        printJevDecision(deliveryMessage, deliveryIntentResult);

        assertThat(productIntentResult.predictedIntent()).isEqualTo(ShoppingIntent.PRODUCT_SEARCH);
        assertThat(deliveryIntentResult.predictedIntent()).isEqualTo(ShoppingIntent.ORDER_DELIVERY);
        for (IntentClassificationResult intentResult : List.of(productIntentResult, deliveryIntentResult)) {
            assertThat(intentResult.confidence()).isBetween(0.0, 1.0);
            assertThat(intentResult.probabilities()).containsKey(intentResult.predictedIntent().name());
            assertThat(intentResult.probabilities().values()).allSatisfy(value -> assertThat(value).isBetween(0.0, 1.0));
        }
    }

    private void printJevDecision(String message, Object result) {
        System.out.println("고객 요청: " + message);
        System.out.println("Jev 판단과 서비스 처리 결과: " + result);
    }
}
