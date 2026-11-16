package com.example.ailearning.l14jev.domain;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class SupportRoutingPolicyUnitTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    // 각 CSV 행을 인자로 받아 같은 검증을 반복합니다. 열 순서는 아래 메서드의 매개변수 순서입니다.
    @ParameterizedTest
    @CsvSource({"PRODUCT_SEARCH, 0.7, false", "PRODUCT_SEARCH, 0.6999, true", "OTHER, 0.99, true"})
    void 분류_신뢰도가_낮거나_기타_의도이면_확인_질문이_필요하다(ShoppingIntent intent,
            double confidence, boolean expected) {
        assertThat(SupportRoutingPolicy.requiresIntentClarification(intent, confidence)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0, 0.9, LOW", "0.4999, 0.7, LOW", "0.5, 0.7, NORMAL", "1.4999, 0.9, NORMAL",
                "1.5, 0.7, HIGH", "2, 0.9, HIGH", "2, 0.6999, REVIEW"})
    void 점수와_신뢰도로_상담_우선순위를_정한다(double score, double confidence, SupportPriority expected) {
        assertThat(SupportRoutingPolicy.determinePriority(score, confidence)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0, CONTINUE_CHAT", "0.2, CONTINUE_CHAT", "0.2001, ASK_CLARIFICATION",
                "0.5, ASK_CLARIFICATION", "0.7999, ASK_CLARIFICATION", "0.8, OFFER_HUMAN_SUPPORT",
                "1, OFFER_HUMAN_SUPPORT"})
    void 상담원_요청_확률의_중간_구간에서는_고객에게_확인한다(double probability, HumanSupportAction expected) {
        assertThat(SupportRoutingPolicy.determineHumanSupportAction(probability)).isEqualTo(expected);
    }
}
