package com.example.shopping.tutor.lesson.l11evaluation;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;
import com.example.shopping.tutor.lesson.l02structured.StructuredOutputLesson;
import com.example.shopping.tutor.lesson.l06tools.support.DemoProductCatalog;
import com.example.shopping.tutor.lesson.l06tools.support.ShippingQuoteCalculator;
import com.example.shopping.tutor.lesson.l09rag.support.PolicyDocumentFixtures;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.evaluation.EvaluationRequest;

import java.util.*;

/** 11 단원: 핵심 API를 직접 호출한다. 웹 실행·세션 확정은 platform에서 담당한다. */
public class EvaluationLesson {

    /** 모델 판정 전에 결정적인 상품·금액 검증을 수행합니다. */
    public Result checkBusinessFacts(String fixture) {
        var results =
                List.of("P004", "P005").stream()
                        .map(
                                id ->
                                        Map.of(
                                                "product",
                                                DemoProductCatalog.findProduct(id),
                                                "quote",
                                                ShippingQuoteCalculator.quote(
                                                        fixture, List.of(id))))
                        .toList();
        return Result.ok("상품 ID·재고·배송그룹·금액은 Java가 검증합니다.", Map.of("verified", results));
    }

    /** 실제 Spring AI Evaluator를 사용하되 사실성 보증으로 오해하지 않습니다. */
    public Result judgeRelevance(ChatModel chatModel, String question) {
        var evaluator = new RelevancyEvaluator(ChatClient.builder(chatModel));
        var evaluation =
                evaluator.evaluate(
                        new EvaluationRequest(
                                question,
                                List.of(PolicyDocumentFixtures.policies().getFirst()),
                                "미개봉 상품은 수령 후 7일 이내 반품 가능합니다."));
        return Result.ok(
                "관련성 판정은 보조 신호입니다. STUB에서는 판정 경로만 확인합니다.",
                Map.of(
                        "pass",
                        evaluation.isPass(),
                        "feedback",
                        String.valueOf(evaluation.getFeedback())));
    }

    /** 정답 라벨과 조건 추출 결과를 비교하고 실패를 보고합니다. */
    public Result runRegressionDataset(ChatClient chatClient) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (var entry :
                Map.of(
                                "3만원 이하 생활용품",
                                new SearchCriteria(null, "생활", 30000L),
                                "15000원 이하 음료",
                                new SearchCriteria(null, "음료", 15000L))
                        .entrySet()) {
            var actual =
                    chatClient
                            .prompt()
                            .system(StructuredOutputLesson.CRITERIA)
                            .user(entry.getKey())
                            .call()
                            .entity(SearchCriteria.class);
            results.add(
                    Map.of(
                            "question",
                            entry.getKey(),
                            "expected",
                            entry.getValue(),
                            "actual",
                            Objects.requireNonNull(actual),
                            "pass",
                            entry.getValue().equals(actual)));
        }
        return Result.ok(
                "질문별 성공과 실패를 모두 표시합니다.",
                Map.of(
                        "results",
                        results,
                        "successes",
                        results.stream().filter(r -> Boolean.TRUE.equals(r.get("pass"))).count()));
    }

    public record Result(String status, String answer, Map<String, Object> data) {
        public static Result ok(String answer, Map<String, Object> data) {
            return new Result("COMPLETED", answer, data);
        }

        public static Result text(String answer) {
            if (answer == null || answer.isBlank())
                throw new IllegalStateException("모델이 빈 응답을 반환했습니다.");
            return ok(answer, Map.of());
        }

        public static Result unsupported(String reason) {
            return new Result("UNSUPPORTED", reason, Map.of());
        }
    }
}
