package com.example.shopping.tutor.lesson.l13shopping;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;
import com.example.shopping.tutor.lesson.l02structured.StructuredOutputLesson;
import com.example.shopping.tutor.lesson.l06tools.support.DemoCartRepository;
import com.example.shopping.tutor.lesson.l06tools.support.DemoProductCatalog;
import com.example.shopping.tutor.lesson.l06tools.support.ShippingPolicy;
import com.example.shopping.tutor.lesson.l06tools.support.ShippingQuoteCalculator;
import com.example.shopping.tutor.lesson.l09rag.PolicyRagLesson;
import com.example.shopping.tutor.lesson.l13shopping.support.FreeShippingRecommendationPolicy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.*;

/** 13 단원: 핵심 API를 직접 호출한다. 웹 실행·세션 확정은 platform에서 담당한다. */
public class ShoppingAssistantLesson {

    private String explain(ChatClient chatClient, String question, Object facts) {
        return chatClient
                .prompt()
                .system(SHOPPING_SYSTEM + " 다음 서버 사실만 설명하세요. 다른 상품이나 가격을 추가하지 마세요.\n" + facts)
                .user(question)
                .call()
                .content();
    }

    public static final String SHOPPING_SYSTEM =
            "당신은 학습용 쇼핑 어시스턴트입니다. 한국어로 간결히 답하세요. 제공하지 않은 가격·재고·정책을 만들어내지 마세요.";

    /** 핵심 동작을 분리해 단원 테스트에서 직접 검증한다. */
    public SearchCriteria update(
            ChatClient chatClient, String question, SearchCriteria previousCriteria) {
        previousCriteria =
                com.example.shopping.tutor.lesson.l04memory.SearchCriteriaReducer.merge(
                        previousCriteria,
                        new StructuredOutputLesson().extract(chatClient, question),
                        question);
        return previousCriteria;
    }

    /** 조건 추출 → Java 병합 → 상품 검색 → 사실 기반 설명을 조합합니다. */
    public Result recommendWithFollowUp(
            ChatClient chatClient, String question, SearchCriteria previousCriteria) {
        var criteria = update(chatClient, question, previousCriteria);
        var products = DemoProductCatalog.search(criteria);
        return new Result(
                products.isEmpty() ? "NO_RESULTS" : "COMPLETED",
                products.isEmpty() ? "조건에 맞는 상품이 없습니다." : explain(chatClient, question, products),
                Map.of("criteria", criteria, "products", products));
    }

    /** 현재 견적·부족액·사용자 예산·후보별 견적을 Java로 검증합니다. */
    public Result recommendFreeShipping(
            ChatClient chatClient,
            String question,
            String fixture,
            SearchCriteria previousCriteria) {
        var criteria = update(chatClient, question, previousCriteria);
        long subtotal = DemoCartRepository.subtotal(fixture);
        long missing = Math.max(0, 50000 - subtotal);
        if (missing == 0)
            return Result.ok(
                    "이미 무료배송 조건입니다.",
                    Map.of("criteria", criteria, "subtotal", subtotal, "products", List.of()));
        long max = FreeShippingRecommendationPolicy.maxPrice(criteria, missing);
        var candidates =
                FreeShippingRecommendationPolicy.candidates(criteria, fixture, missing, max);
        var quotes =
                candidates.stream()
                        .map(
                                product ->
                                        Map.of(
                                                "product",
                                                product,
                                                "quote",
                                                ShippingQuoteCalculator.quote(
                                                        fixture, List.of(product.id()))))
                        .toList();
        Map<String, Object> facts =
                Map.of(
                        "currentSubtotal",
                        subtotal,
                        "currentShippingFee",
                        ShippingPolicy.shippingFee(subtotal),
                        "missing",
                        missing,
                        "threshold",
                        50000,
                        "hypotheticalAlternatives",
                        quotes);
        String answer = "조건에 맞는 무료배송 후보가 없습니다.";
        if (!candidates.isEmpty()) {
            String guide =
                    chatClient
                            .prompt()
                            .system(
                                    SHOPPING_SYSTEM
                                            + "\n"
                                            + "현재 장바구니는 무료배송 기준에 미달합니다. 아래 견적은 후보 하나를 추가했을 때의"
                                            + " 가정입니다. 아직 실제 추가하지 않았습니다. 상품의 선택 기준만 간단히 설명하세요. 현재"
                                            + " 무료배송이라고 말하거나 가격·배송비를 다시 계산하지 마세요.\n"
                                            + facts)
                            .user(question)
                            .call()
                            .content();
            answer =
                    String.format(
                            java.util.Locale.KOREA,
                            "현재 장바구니 %,d원 · 배송비 %,d원 · 무료배송까지 %,d원 부족합니다.\n"
                                    + "아래 후보 중 하나를 1개 추가하면 무료배송 조건을 충족합니다. 실제 장바구니는 변경하지 않았습니다.\n\n"
                                    + "%s",
                            subtotal,
                            ShippingPolicy.shippingFee(subtotal),
                            missing,
                            guide);
        }
        return new Result(
                candidates.isEmpty() ? "NO_RESULTS" : "COMPLETED",
                answer,
                Map.of(
                        "criteria",
                        criteria,
                        "missing",
                        missing,
                        "maxPrice",
                        max,
                        "products",
                        candidates,
                        "quotes",
                        quotes,
                        "currentSubtotal",
                        subtotal,
                        "currentShippingFee",
                        ShippingPolicy.shippingFee(subtotal)));
    }

    /** 상품 사실과 문서 근거를 각각 조회해 조합합니다. */
    public Result combinePolicyAndCatalog(
            ChatClient chatClient,
            String question,
            String experiment,
            String conversationId,
            EmbeddingModel embeddingModel,
            ChatMemory chatMemory,
            int topK,
            java.util.function.BiConsumer<String, Map<String, Object>> events) {
        var products = DemoProductCatalog.search(new SearchCriteria("텀블러", null, null));
        var policy =
                new PolicyRagLesson()
                        .answer(
                                chatClient,
                                question,
                                experiment,
                                conversationId,
                                embeddingModel,
                                chatMemory,
                                topK,
                                events,
                                question);
        return new Result(
                policy.status(),
                policy.answer(),
                Map.of("products", products, "policy", policy.data()));
    }

    /** 실패 턴은 상태를 확정하지 않습니다. 실패 실험 후 이전 조건을 확인하세요. */
    public Result recoverAndReset(
            ChatClient chatClient,
            String question,
            String experiment,
            SearchCriteria previousCriteria) {
        Result result = recommendWithFollowUp(chatClient, question, previousCriteria);
        if (experiment.equals("timeout"))
            throw new IllegalStateException("학습용 실패: 변경한 조건을 commit하지 않습니다.");
        return result;
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
