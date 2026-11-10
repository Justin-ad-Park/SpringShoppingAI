package com.example.ailearning.l13intenteval;

import com.example.ailearning.l13intent.IntentClassificationApplication;
import com.example.ailearning.l13intent.IntentClassificationService;
import com.example.ailearning.l13intent.domain.IntentCatalog;
import com.example.ailearning.testsupport.TestNamePrinter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 OpenAI API를 모델별로 최대 5회씩 호출합니다. API 사용량과 비용이 발생합니다.
@SpringBootTest(classes = IntentClassificationApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.ai.openai.chat.timeout=45s")
class IntentModelQualityOpenAiLiveTest {
    // 공식 가격표의 낮은 비용대에서 고른 모델입니다. 계정별 사용 가능 여부는 다를 수 있습니다.
    private static final List<String> MODELS = List.of(
            "gpt-6-luna",
            "gpt-5.6-luna",
            "gpt-5.4-mini");

    @Autowired
    private ChatClient.Builder chatClientBuilder;
    @Autowired
    private IntentCatalog intentCatalog;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    @Timeout(900)
    void 낮은_비용의_세_모델을_같은_질문으로_비교하고_정답_일치율을_출력한다() throws IOException {
        List<EvaluationCase> cases = readCases();
        assertThat(cases).isNotEmpty().hasSizeLessThanOrEqualTo(5);
        validateCases(cases);

        List<ModelReport> reports = new ArrayList<>();
        for (String model : MODELS) {
            var classifier = new IntentClassificationService(
                    chatClientBuilder.clone()
                            .defaultOptions(OpenAiChatOptions.builder().model(model)),
                    intentCatalog,
                    objectMapper);
            List<CaseResult> results = cases.stream()
                    .map(testCase -> evaluateOne(classifier, testCase))
                    .toList();
            reports.add(new ModelReport(model, results));
        }

        printReports(reports);
        assertThat(reports).hasSize(MODELS.size());
        assertThat(reports).allSatisfy(report ->
                assertThat(report.successfulResponses()).as(report.model() + " 유효한 응답").isPositive());
    }

    private List<EvaluationCase> readCases() throws IOException {
        try (var input = new ClassPathResource("intent-evaluation-cases.json").getInputStream()) {
            return objectMapper.readValue(input,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, EvaluationCase.class));
        }
    }

    private void validateCases(List<EvaluationCase> cases) {
        for (EvaluationCase testCase : cases) {
            if (testCase.expectedDecision() == null) {
                throw new IllegalStateException(testCase.id() + " 문항에 expected_decision이 없습니다.");
            }
            if (testCase.expectedDecision() == IntentClassificationService.Decision.MATCH
                    && (testCase.expectedMainIntent() == null || testCase.expectedSubIntent() == null)) {
                throw new IllegalStateException(testCase.id() + " MATCH 문항에 대분류·소분류 정답이 필요합니다.");
            }
            if (testCase.expectedDecision() != IntentClassificationService.Decision.MATCH
                    && (testCase.expectedMainIntent() != null || testCase.expectedSubIntent() != null)) {
                throw new IllegalStateException(testCase.id() + " 보류 문항에는 대분류·소분류 정답을 넣을 수 없습니다.");
            }
        }
    }

    private CaseResult evaluateOne(IntentClassificationService classifier, EvaluationCase testCase) {
        try {
            var actual = classifier.classify(testCase.question());
            boolean correct = testCase.expectedDecision() == actual.decision()
                    && Objects.equals(testCase.expectedMainIntent(), actual.mainIntent())
                    && Objects.equals(testCase.expectedSubIntent(), actual.subIntent());
            return new CaseResult(testCase, actual.decision().name(), actual.mainIntent(), actual.subIntent(), correct, null);
        } catch (RuntimeException error) {
            // 한 문항의 변환·호출 실패를 기록하고 나머지 문항의 비교를 계속합니다.
            String cause = error.getCause() == null
                    ? error.getClass().getSimpleName()
                    : error.getCause().getClass().getSimpleName();
            return new CaseResult(testCase, null, null, null, false, cause);
        }
    }

    private void printReports(List<ModelReport> reports) {
        System.out.println("\n===== 의도 분류 모델 비교 =====");
        for (ModelReport report : reports) {
            System.out.printf("\n모델: %s | 정답 일치: %d/%d | 정확도: %.0f%% | 유효 응답: %d/%d%n",
                    report.model(), report.correctCount(), report.results().size(), report.accuracyPercent(),
                    report.successfulResponses(), report.results().size());
            for (CaseResult result : report.results()) {
                System.out.printf("  %s | 질문=%s | 기대=%s | 결과=%s | 판정=%s%n",
                        result.testCase().id(), result.testCase().question(), result.expectedLabel(), result.actualLabel(),
                        result.error() == null ? (result.correct() ? "정답" : "오답") : "오류(" + result.error() + ")");
            }
        }
        System.out.println("\n정확도는 이 고정된 교육용 질문에 대한 정답 조합 일치율입니다. 전체 의도 분류 품질을 대표하지 않습니다.");
    }

    private record EvaluationCase(String id, String question,
                                  @JsonProperty("expected_decision") IntentClassificationService.Decision expectedDecision,
                                  @JsonProperty("expected_main_intent") String expectedMainIntent,
                                  @JsonProperty("expected_sub_intent") String expectedSubIntent) {
        String expectedLabel() {
            return expectedDecision == IntentClassificationService.Decision.MATCH
                    ? expectedMainIntent + ":" + expectedSubIntent
                    : expectedDecision.name();
        }
    }

    private record CaseResult(EvaluationCase testCase, String decision, String mainIntent, String subIntent,
                              boolean correct, String error) {
        String expectedLabel() {
            return testCase.expectedLabel();
        }

        String actualLabel() {
            if (error != null) return "호출/응답 오류";
            return decision.equals(IntentClassificationService.Decision.MATCH.name())
                    ? mainIntent + ":" + subIntent
                    : decision;
        }
    }

    private record ModelReport(String model, List<CaseResult> results) {
        long correctCount() {
            return results.stream().filter(CaseResult::correct).count();
        }

        long successfulResponses() {
            return results.stream().filter(result -> result.error() == null).count();
        }

        double accuracyPercent() {
            return results.isEmpty() ? 0 : correctCount() * 100.0 / results.size();
        }
    }
}
