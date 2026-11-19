package com.example.ailearning.l12evaluation.rules;

import java.util.List;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedEvaluatorUnitTest {
    private final RuleBasedEvaluator evaluator = new RuleBasedEvaluator();

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) { TestNamePrinter.printTestName(testInfo); }

    @Test
    void 기대한_상태와_출처와_문구가_있으면_통과한다() {
        var expected = new RuleBasedEvaluator.ExpectedAnswer("ANSWERED", List.of("return-sealed"), "7일");
        var actual = new RuleBasedEvaluator.CandidateAnswer("ANSWERED", List.of("return-sealed"), "반품은 7일 이내입니다.");
        var failures = evaluator.evaluate(expected, actual);
        System.out.printf("후보 답변: %s%n실패 사유: %s%n", actual, failures);
        assertThat(failures).isEmpty();
    }

    @Test
    void 출처가_맞아도_기대_문구가_없으면_실패한다() {
        var expected = new RuleBasedEvaluator.ExpectedAnswer("ANSWERED", List.of("return-sealed"), "7일");
        var actual = new RuleBasedEvaluator.CandidateAnswer("ANSWERED", List.of("return-sealed"), "반품은 30일 이내입니다.");
        var failures = evaluator.evaluate(expected, actual);
        System.out.printf("후보 답변: %s%n실패 사유: %s%n", actual, failures);
        assertThat(failures).containsExactly("expected text missing");
    }
}
