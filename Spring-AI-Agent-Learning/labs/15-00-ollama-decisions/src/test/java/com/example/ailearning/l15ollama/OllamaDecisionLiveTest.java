package com.example.ailearning.l15ollama;

import com.example.ailearning.l15ollama.domain.ShoppingIntent;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

/** 실제 로컬 Ollama와 다운로드한 의사결정 모델이 필요하다. OpenAI·Jev는 호출하지 않는다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Timeout(180)
class OllamaDecisionLiveTest {
    @Autowired IntentClassificationService intentClassificationService;
    @Autowired HumanSupportRequestService humanSupportRequestService;
    @Autowired SupportPriorityService supportPriorityService;
    @Autowired CustomerAssessmentService customerAssessmentService;
    @Autowired OrderContextDecisionService orderContextDecisionService;

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

    @Test
    void 실제_Ollama가_상담원_연결_요청과_거절을_구분한다() {
        var requested = humanSupportRequestService.assessHumanSupportRequest("사람 상담원과 직접 이야기하고 싶어요. 연결해 주세요.");
        var declined = humanSupportRequestService.assessHumanSupportRequest("상담원 연결은 원하지 않아요. 자동 안내만 해 주세요.");
        System.out.println("연결 요청: " + requested);
        System.out.println("자동 안내 요청: " + declined);
        assertThat(requested.yesProbability()).isBetween(0.0, 1.0);
        assertThat(declined.yesProbability()).isBetween(0.0, 1.0);
        assertThat(requested.yesProbability()).isGreaterThan(declined.yesProbability());
    }

    @Test
    void 실제_Ollama가_일반_문의보다_임박한_마감의_시급성을_높게_평가한다() {
        var routine = supportPriorityService.assessPriority("급한 문제는 없고 텀블러 재질이 궁금해요.");
        var urgent = supportPriorityService.assessPriority("취소 마감이 오늘 한 시간 뒤입니다. 아직 취소가 안 돼 즉시 확인이 필요해요.");
        System.out.println("일반 문의: " + routine);
        System.out.println("마감 임박: " + urgent);
        assertThat(routine.score()).isBetween(0.0, 2.0);
        assertThat(urgent.score()).isBetween(0.0, 2.0);
        assertThat(urgent.score()).isGreaterThan(routine.score());
    }

    @Test
    void 실제_Ollama가_하나의_메시지에_대한_세_질문에_답한다() {
        var result = customerAssessmentService.assessCustomerMessage("배송이 지연되고 있어요. 오늘 꼭 받아야 하니 사람 상담원에게 연결해 주세요.");
        System.out.println("세 질문의 결과: " + result);
        assertThat(result.intent()).isEqualTo(ShoppingIntent.ORDER_DELIVERY);
        assertThat(result.humanSupportProbability()).isBetween(0.0, 1.0);
        assertThat(result.urgencyScore()).isBetween(0.0, 2.0);
    }

    @Test
    void 실제_Ollama가_주문_상태와_고객_메시지를_함께_판단한다() {
        var inquiry = new OrderContextDecisionService.OrderInquiry("배송된 상품을 반품하고 싶어요.", "DELIVERED", 2);
        var result = orderContextDecisionService.decideNextAction(inquiry);
        System.out.println("주문 정보 입력: " + inquiry);
        System.out.println("다음 안내 판단: " + result);
        assertThat(result.nextAction()).isEqualTo("EXPLAIN_RETURN");
        assertThat(result.probabilities()).containsKey(result.nextAction());
    }
}
