package com.example.ailearning.l14jev.lesson02_score;

import com.example.ailearning.l14jev.lesson02_score.Lesson02SupportPriorityService.SupportPriorityResult;
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
class Lesson02JevScoreLiveTest {
    @Autowired Lesson02SupportPriorityService priorityService;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void Score로_일반_문의보다_마감이_임박한_문의의_시급성을_높게_평가한다() {
        String routineMessage = "급한 문제는 없고, 구매 전에 텀블러 재질이 궁금해요.";
        SupportPriorityResult routinePriorityResult = priorityService.assessPriority(routineMessage);
        printJevDecision(routineMessage, routinePriorityResult);
        String urgentMessage = "주문 취소 마감이 오늘 한 시간 뒤인데 아직 처리가 안 됐어요. 바로 확인해 주세요.";
        SupportPriorityResult urgentPriorityResult = priorityService.assessPriority(urgentMessage);
        printJevDecision(urgentMessage, urgentPriorityResult);

        for (SupportPriorityResult priorityResult : List.of(routinePriorityResult, urgentPriorityResult)) {
            assertThat(priorityResult.score()).isBetween(0.0, 2.0);
            assertThat(priorityResult.confidence()).isBetween(0.0, 1.0);
            assertThat(priorityResult.probabilities()).containsKeys(0, 1, 2);
            assertThat(priorityResult.probabilities().values()).allSatisfy(value -> assertThat(value).isBetween(0.0, 1.0));
            assertThat(priorityResult.priority()).isNotNull();
        }
        // 실제 소수 점수나 confidence를 고정하지 않고 두 입력의 상대적인 시급성을 비교합니다.
        assertThat(urgentPriorityResult.score()).isGreaterThan(routinePriorityResult.score());
    }

    private void printJevDecision(String message, Object result) {
        System.out.println("고객 요청: " + message);
        System.out.println("Jev 판단과 서비스 처리 결과: " + result);
    }
}
