package com.example.ailearning.l15ollama.domain;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;

class SupportRoutingPolicyUnitTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 기타_또는_불확실한_의도는_확인을_요청한다() {
        System.out.println("PRODUCT_SEARCH·confidence 0.69의 확인 필요 여부: "
                + SupportRoutingPolicy.requiresIntentClarification(ShoppingIntent.PRODUCT_SEARCH, 0.69));
        assertThat(SupportRoutingPolicy.requiresIntentClarification(ShoppingIntent.OTHER, 0.9)).isTrue();
        assertThat(SupportRoutingPolicy.requiresIntentClarification(ShoppingIntent.PRODUCT_SEARCH, 0.69)).isTrue();
        assertThat(SupportRoutingPolicy.requiresIntentClarification(ShoppingIntent.PRODUCT_SEARCH, null)).isTrue();
        assertThat(SupportRoutingPolicy.requiresIntentClarification(ShoppingIntent.PRODUCT_SEARCH, 0.7)).isFalse();
    }

    @Test
    void 점수와_confidence로_상담_우선순위를_정한다() {
        System.out.println("점수 1.5·confidence 0.7의 우선순위: "
                + SupportRoutingPolicy.determinePriority(1.5, 0.7));
        assertThat(SupportRoutingPolicy.determinePriority(1.6, 0.69)).isEqualTo(SupportPriority.REVIEW);
        assertThat(SupportRoutingPolicy.determinePriority(1.6, null)).isEqualTo(SupportPriority.REVIEW);
        assertThat(SupportRoutingPolicy.determinePriority(0.49, 0.7)).isEqualTo(SupportPriority.LOW);
        assertThat(SupportRoutingPolicy.determinePriority(0.5, 0.7)).isEqualTo(SupportPriority.NORMAL);
        assertThat(SupportRoutingPolicy.determinePriority(1.5, 0.7)).isEqualTo(SupportPriority.HIGH);
    }

    @Test
    void 상담원_요청의_중간_확률은_확인_질문으로_처리한다() {
        System.out.println("상담원 요청 확률 0.5의 안내 방식: "
                + SupportRoutingPolicy.determineHumanSupportAction(0.5));
        assertThat(SupportRoutingPolicy.determineHumanSupportAction(0.2)).isEqualTo(HumanSupportAction.CONTINUE_CHAT);
        assertThat(SupportRoutingPolicy.determineHumanSupportAction(0.5)).isEqualTo(HumanSupportAction.ASK_CLARIFICATION);
        assertThat(SupportRoutingPolicy.determineHumanSupportAction(0.8)).isEqualTo(HumanSupportAction.OFFER_HUMAN_SUPPORT);
    }
}
