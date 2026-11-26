package com.example.ailearning.l12evaluation.lesson01_cases;

import java.util.List;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;

class Lesson01RuleBasedEvaluatorUnitTest {
    private final Lesson01RuleBasedEvaluator evaluator = new Lesson01RuleBasedEvaluator();

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) { TestNamePrinter.printTestName(testInfo); }

    @Test
    void 기대한_상태와_출처와_문구가_있으면_통과한다() {
        var expected = new Lesson01RuleBasedEvaluator.ExpectedAnswer("ANSWERED", List.of("return-sealed"), "7일");
        var actual = new Lesson01RuleBasedEvaluator.CandidateAnswer("ANSWERED", List.of("return-sealed"), "반품은 7일 이내입니다.");
        var failures = evaluator.evaluate(expected, actual);
        System.out.printf("후보 답변: %s%n실패 사유: %s%n", actual, failures);
        assertThat(failures).isEmpty();
    }

    @Test
    void 출처가_맞아도_기대_문구가_없으면_실패한다() {
        var expected = new Lesson01RuleBasedEvaluator.ExpectedAnswer("ANSWERED", List.of("return-sealed"), "7일");
        var actual = new Lesson01RuleBasedEvaluator.CandidateAnswer("ANSWERED", List.of("return-sealed"), "반품은 30일 이내입니다.");
        var failures = evaluator.evaluate(expected, actual);
        System.out.printf("후보 답변: %s%n실패 사유: %s%n", actual, failures);
        assertThat(failures).containsExactly("expected text missing");
    }

    @Test
    void 오답도_필수_문구를_포함하면_규칙_검사를_통과한다() {
        var expected = new Lesson01RuleBasedEvaluator.ExpectedAnswer("ANSWERED", List.of("return-sealed"), "7일");
        var actual = new Lesson01RuleBasedEvaluator.CandidateAnswer(
                "ANSWERED", List.of("return-sealed"), "반품은 7일이 아니라 30일입니다.");

        var failures = evaluator.evaluate(expected, actual);

        System.out.printf("후보 답변: %s%n실패 사유: %s%n", actual, failures);
        assertThat(failures).isEmpty();
    }
}
