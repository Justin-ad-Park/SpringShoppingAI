package com.example.ailearning.l14jev;

import com.example.ailearning.l14jev.domain.ShoppingIntent;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Jev API 호출: 루트 .env의 TYPESAFE_API_KEY가 필요하며 사용량이 발생합니다.
 * 세 테스트가 각각 두 번 호출하므로 전체 실행 시 총 여섯 번 요청합니다. OpenAI는 호출하지 않습니다.
 * 고정 응답 테스트와 달리 실제 모델의 판단이 기대와 다르면 실패할 수 있습니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class JevLiveTest {
    @Autowired IntentClassificationService intentService;
    @Autowired SupportPriorityService priorityService;
    @Autowired HumanSupportService humanSupportService;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void Choice로_상품_추천과_주문_배송_문의의_의도를_구분한다() {
        String productMessage = "3만원 이하의 가벼운 텀블러를 추천해 주세요.";
        var productResult = intentService.classify(productMessage);
        printResult(productMessage, productResult);
        String deliveryMessage = "이미 주문한 상품이 아직 도착하지 않았어요. 지금 배송 상태가 궁금합니다.";
        var deliveryResult = intentService.classify(deliveryMessage);
        printResult(deliveryMessage, deliveryResult);

        assertThat(productResult.predictedIntent()).isEqualTo(ShoppingIntent.PRODUCT_SEARCH);
        assertThat(deliveryResult.predictedIntent()).isEqualTo(ShoppingIntent.ORDER_DELIVERY);
        for (var result : java.util.List.of(productResult, deliveryResult)) {
            assertThat(result.confidence()).isBetween(0.0, 1.0);
            assertThat(result.probabilities()).containsKey(result.predictedIntent().name());
            assertThat(result.probabilities().values()).allSatisfy(value -> assertThat(value).isBetween(0.0, 1.0));
        }
    }

    @Test
    void Score로_일반_문의보다_마감이_임박한_문의의_시급성을_높게_평가한다() {
        String routineMessage = "급한 문제는 없고, 구매 전에 텀블러 재질이 궁금해요.";
        var routineResult = priorityService.assess(routineMessage);
        printResult(routineMessage, routineResult);
        String urgentMessage = "주문 취소 마감이 오늘 한 시간 뒤인데 아직 처리가 안 됐어요. 바로 확인해 주세요.";
        var urgentResult = priorityService.assess(urgentMessage);
        printResult(urgentMessage, urgentResult);

        for (var result : java.util.List.of(routineResult, urgentResult)) {
            assertThat(result.score()).isBetween(0.0, 2.0);
            assertThat(result.confidence()).isBetween(0.0, 1.0);
            assertThat(result.probabilities()).containsKeys(0, 1, 2);
            assertThat(result.probabilities().values()).allSatisfy(value -> assertThat(value).isBetween(0.0, 1.0));
            assertThat(result.priority()).isNotNull();
        }
        // 실제 소수 점수나 confidence를 고정하지 않고 두 입력의 상대적인 시급성을 비교합니다.
        assertThat(urgentResult.score()).isGreaterThan(routineResult.score());
    }

    @Test
    void Noul로_상담원_연결을_원하는_요청과_원하지_않는_요청을_구분한다() {
        String requestedMessage = "자동 응답 말고 사람 상담원과 직접 이야기하고 싶어요. 연결해 주세요.";
        var requestedResult = humanSupportService.assess(requestedMessage);
        printResult(requestedMessage, requestedResult);
        String notRequestedMessage = "상담원 연결은 원하지 않아요. 자동 안내로 텀블러 세척 방법만 알려 주세요.";
        var notRequestedResult = humanSupportService.assess(notRequestedMessage);
        printResult(notRequestedMessage, notRequestedResult);

        assertThat(requestedResult.yesProbability()).isBetween(0.0, 1.0);
        assertThat(notRequestedResult.yesProbability()).isBetween(0.0, 1.0);
        assertThat(requestedResult.action()).isNotNull();
        assertThat(notRequestedResult.action()).isNotNull();
        // 모델의 실제 확률을 관찰합니다. 서비스 임계값의 경계는 별도 정책 단위 테스트에서 검증합니다.
        assertThat(requestedResult.yesProbability()).isGreaterThan(notRequestedResult.yesProbability());
    }

    private void printResult(String message, Object result) {
        System.out.println("고객 요청: " + message);
        System.out.println("Jev 판단과 서비스 처리 결과: " + result);
    }
}
