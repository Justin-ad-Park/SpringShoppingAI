package com.example.ailearning.l14jev.lesson03_noul;

import com.example.ailearning.l14jev.lesson03_noul.Lesson03HumanSupportRequestService.HumanSupportRequestResult;
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
class Lesson03JevNoulLiveTest {
    @Autowired Lesson03HumanSupportRequestService humanSupportRequestService;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void Noul로_상담원_연결을_원하는_요청과_원하지_않는_요청을_구분한다() {
        String requestedMessage = "자동 응답 말고 사람 상담원과 직접 이야기하고 싶어요. 연결해 주세요.";
        HumanSupportRequestResult requestedHumanSupportResult = humanSupportRequestService.assessHumanSupportRequest(requestedMessage);
        printJevDecision(requestedMessage, requestedHumanSupportResult);
        String notRequestedMessage = "상담원 연결은 원하지 않아요. 자동 안내로 텀블러 세척 방법만 알려 주세요.";
        HumanSupportRequestResult notRequestedHumanSupportResult = humanSupportRequestService.assessHumanSupportRequest(notRequestedMessage);
        printJevDecision(notRequestedMessage, notRequestedHumanSupportResult);

        assertThat(requestedHumanSupportResult.yesProbability()).isBetween(0.0, 1.0);
        assertThat(notRequestedHumanSupportResult.yesProbability()).isBetween(0.0, 1.0);
        assertThat(requestedHumanSupportResult.action()).isNotNull();
        assertThat(notRequestedHumanSupportResult.action()).isNotNull();
        // 모델의 실제 확률을 관찰합니다. 서비스 임계값의 경계는 별도 정책 단위 테스트에서 검증합니다.
        assertThat(requestedHumanSupportResult.yesProbability()).isGreaterThan(notRequestedHumanSupportResult.yesProbability());
    }

    private void printJevDecision(String message, Object result) {
        System.out.println("고객 요청: " + message);
        System.out.println("Jev 판단과 서비스 처리 결과: " + result);
    }
}
