package com.example.shopping.integration;

import com.example.shopping.integration.support.ChatClient;
import com.example.shopping.integration.support.IntegrationSuite;
import com.example.shopping.integration.support.TestMode;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.example.shopping.integration.support.ChatClient.ids;
import static com.example.shopping.integration.support.ChatClient.tools;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Java 규칙 기반 해석기로 쇼핑 기능과 대화 상태를 검증한다.
 * 각 테스트가 필요한 대화를 직접 준비하므로 메서드 실행 순서에 의존하지 않는다.
 */
@Tag("smoke")
@DisplayName("Mock 쇼핑 통합 테스트")
class MockIntegrationTest extends IntegrationSuite {
    @Override
    protected TestMode defaultMode() {
        return TestMode.MOCK;
    }

    @BeforeAll
    void requireMockMode() throws Exception {
        try (var check = new ChatClient(environment.baseUrl())) {
            assertThat(check.status().path("mode").asText()).isEqualTo("mock");
        }
    }

    /** 최초 추천의 상품, 배송비, 기본 가격 범위와 도구 호출 수를 검증한다. */
    @Test
    @DisplayName("최초 무료배송 추천은 기본 가격 범위에서 배송비가 없는 상품을 반환한다")
    void initialFreeShippingRecommendation() throws Exception {
        var result = client.conversation().say("무료배송 상품을 추천해줘");

        // 기본 데이터는 무료배송까지 8천 원 부족하며 기본 상한은 1만 5천 원이다.
        assertThat(ids(result)).containsExactly("P003", "P004", "P005");
        assertThat(result.path("toolCalls")).hasSize(7);
        result.path("products").forEach(product ->
                assertThat(product.at("/shipping/shippingFee").asInt()).isZero());
        assertThat(result.at("/details/minPrice").asInt()).isEqualTo(8000);
        assertThat(result.at("/details/maxPrice").asInt()).isEqualTo(15000);
    }

    /** 후속 카테고리 요청이 새 작업을 만들지 않고 기존 추천을 좁히는지 확인한다. */
    @Test
    @DisplayName("후속 카테고리 요청은 같은 작업에서 음료만 추천한다")
    void categoryRefinementKeepsTask() throws Exception {
        var conversation = client.conversation();
        var initial = conversation.say("무료배송 상품을 추천해줘");

        var result = conversation.say("음료 카테고리에서만 추천해줘");

        assertThat(ids(result)).containsExactly("P004", "P005");
        assertTaskId(result, initial.at("/task/taskId").asText());
    }

    /** 예산 설정 시 기존 카테고리를 유지하고, 카테고리 교체 시 그 예산을 유지한다. */
    @Test
    @DisplayName("카테고리와 예산은 후속 요청에서 누적되며 변경하지 않은 조건을 유지한다")
    void categoryAndBudgetAreRetained() throws Exception {
        var conversation = startFreeShippingConversation();
        conversation.say("음료 카테고리에서만 추천해줘");

        var budget = conversation.say("예산은 3만원까지");
        assertThat(budget.at("/task/constraints/maxPrice").asInt()).isEqualTo(30000);
        assertThat(budget.at("/task/constraints/category").asText()).isEqualTo("음료");

        var living = conversation.say("생활 카테고리에서만 추천해줘");
        assertThat(ids(living)).containsExactly("P009");
        assertThat(living.at("/task/constraints/maxPrice").asInt()).isEqualTo(30000);
        assertTaskId(living, budget.at("/task/taskId").asText());
    }

    /** 독립적인 혜택 조회가 활성 작업을 대체하거나 예산을 없애지 않는지 검증한다. */
    @Test
    @DisplayName("쿠폰·적립금 조회 후에도 기존 추천 작업과 예산으로 검색을 이어간다")
    void benefitsLookupPreservesSearch() throws Exception {
        var conversation = startFreeShippingConversation();
        conversation.say("음료 카테고리에서만 추천해줘");
        var budget = conversation.say("예산은 3만원까지");

        var lookup = conversation.say("쿠폰과 적립금 조회");
        assertThat(lookup.path("action").asText()).isEqualTo("INDEPENDENT");
        assertTaskId(lookup, budget.at("/task/taskId").asText());
        assertThat(lookup.at("/details/points/balance").asInt()).isEqualTo(3500);
        assertThat(lookup.at("/details/coupons/coupons/0/discount").asInt()).isEqualTo(2000);
        assertThat(lookup.at("/task/constraints/category").asText()).isEqualTo("음료");

        // 조회 직후 응답뿐 아니라 다음 검색에서도 예산이 보존되었는지 확인한다.
        var continued = conversation.say("생활 카테고리에서만 추천해줘");
        assertThat(ids(continued)).containsExactly("P009");
        assertThat(continued.at("/task/constraints/maxPrice").asInt()).isEqualTo(30000);
        assertTaskId(continued, budget.at("/task/taskId").asText());
    }

    /** 예산만 지우고 카테고리는 유지한다. 기본 가격 범위에 생활 상품은 없다. */
    @Test
    @DisplayName("예산 해제는 카테고리를 유지하면서 기본 가격 상한으로 돌아간다")
    void clearingBudgetRestoresDefaultLimit() throws Exception {
        var conversation = startFreeShippingConversation();
        conversation.say("예산은 3만원까지");
        conversation.say("생활 카테고리에서만 추천해줘");

        var result = conversation.say("예산 해제");

        assertThat(ids(result)).isEmpty();
        assertThat(result.at("/details/maxPrice").asInt()).isEqualTo(15000);
        assertThat(result.at("/task/constraints/maxPrice").isNull()).isTrue();
        assertThat(result.at("/task/constraints/category").asText()).isEqualTo("생활");
    }

    /** '새로'라는 발화가 기존 무료배송 업무를 일반 검색으로 교체하는지 확인한다. */
    @Test
    @DisplayName("새 일반 검색은 이전 카테고리와 무료배송 제한을 상속하지 않는다")
    void newCatalogSearchReplacesTask() throws Exception {
        var conversation = startFreeShippingConversation();
        conversation.say("음료 카테고리에서만 추천해줘");
        var previous = conversation.say("예산은 7000원까지");

        var result = conversation.say("새로 3만원 이하 텀블러 찾아줘");

        assertThat(ids(result)).containsExactly("P009");
        assertThat(result.at("/products/0/shipping").isNull()).isTrue();
        assertThat(result.at("/task/taskId")).isNotEqualTo(previous.at("/task/taskId"));
        assertThat(result.at("/task/constraints/category").isNull()).isTrue();
        assertThat(result.at("/task/constraints/maxPrice").asInt()).isEqualTo(30000);
        assertThat(tools(result)).containsExactly("searchProducts");
        assertThat(result.at("/toolCalls/0/arguments/query/shippingGroups")).isEmpty();
    }

    /** 새 검색 버튼의 newSearch 플래그로 기존 키워드·카테고리·예산을 지우는지 검증한다. */
    @Test
    @DisplayName("새 검색 플래그는 기존 키워드·카테고리·예산을 초기화한다")
    void newSearchFlagClearsConstraints() throws Exception {
        var conversation = client.conversation();
        conversation.say("새로 3만원 이하 텀블러 찾아줘");
        var previous = conversation.say("생활 카테고리에서만 추천해줘");

        var result = conversation.send("무료배송 상품 추천해줘", "standard", true);

        assertThat(ids(result)).containsExactly("P003", "P004", "P005");
        assertThat(result.at("/task/taskId")).isNotEqualTo(previous.at("/task/taskId"));
        // 서비스 계약에서 초기 키워드는 null이 아닌 빈 문자열이다.
        assertThat(result.at("/task/constraints/keyword").textValue()).isEmpty();
        assertThat(result.at("/task/constraints/category").isNull()).isTrue();
        assertThat(result.at("/task/constraints/maxPrice").isNull()).isTrue();
    }

    /** 장바구니에 담긴 상품도 일반 검색에서는 검색 대상인지 확인한다. */
    @Test
    @DisplayName("일반 키워드 검색으로 그래놀라를 찾는다")
    void catalogSearch() throws Exception {
        var result = client.conversation().say("새로 그래놀라 찾아줘");

        assertThat(ids(result)).containsExactly("P001");
    }

    /** 장바구니별 고정 시나리오를 사용해 결과가 없는 이유까지 확인한다. */
    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource({"already-free, 이미 무료배송", "empty, 비어", "no-match, 없습니다"})
    @DisplayName("장바구니 예외 상황은 상품 없이 이유를 안내한다")
    void shippingEdgeCases(String scenario, String expectedMessage) throws Exception {
        var result = client.conversation().send("무료배송 상품 추천해줘", scenario, false);

        assertThat(ids(result)).isEmpty();
        assertThat(result.path("message").asText()).contains(expectedMessage);
    }

    /** 추천 가능한 최소 금액보다 예산이 작으면 결과 없음으로 처리하는지 검증한다. */
    @Test
    @DisplayName("무료배송 부족 금액보다 예산이 작으면 결과가 없다")
    void insufficientBudget() throws Exception {
        var result = client.conversation().say("무료배송 상품 추천해줘. 예산은 7000원 이하야");

        assertThat(ids(result)).isEmpty();
        assertThat(result.path("outcome").asText()).isEqualTo("NO_RESULTS");
    }

    /** 정책 질문이 상품 검색을 실행하지 않고 무료배송 업무 맥락을 유지하는지 확인한다. */
    @Test
    @DisplayName("배송 정책 질문은 정책 도구만 호출하며 추천 맥락을 유지한다")
    void shippingPolicy() throws Exception {
        var conversation = client.conversation();
        conversation.say("무료배송 상품 추천해줘");

        var policy = conversation.say("무료배송 조건이 뭐야?");

        assertThat(tools(policy)).containsExactly("getShippingPolicy");
        assertThat(policy.at("/task/capabilityId").asText()).isEqualTo("cart.freeShippingRecommendation");
    }

    /** 같은 공개 대화 ID를 사용해도 쿠키가 다르면 작업 상태가 격리되어야 한다. */
    @Test
    @DisplayName("대화 ID가 같아도 다른 쿠키 세션과 상태를 공유하지 않는다")
    void sessionIsolation() throws Exception {
        client.conversation("shared-id").say("무료배송 상품 추천해줘");

        try (var otherBrowser = new ChatClient(environment.baseUrl())) {
            var isolated = otherBrowser.conversation("shared-id").say("음료만");

            assertThat(isolated.path("outcome").asText()).isEqualTo("NEEDS_INPUT");
            assertThat(isolated.path("task").isNull()).isTrue();
        }
    }

    /** 세 종류의 잘못된 입력을 서버가 클라이언트 오류로 거부하는지 확인한다. */
    @Test
    @DisplayName("잘못된 시나리오와 공백·장문 입력은 HTTP 400이다")
    void invalidInput() throws Exception {
        var conversation = client.conversation();

        assertThat(conversation.requestStatus("무료배송 상품 추천해줘", "invalid", 0)).isEqualTo(400);
        assertThat(conversation.requestStatus("   ", "standard", 0)).isEqualTo(400);
        assertThat(conversation.requestStatus("x".repeat(2001), "standard", 0)).isEqualTo(400);
    }

    /** 정상 요청 후 같은 버전을 재전송해 낙관적 잠금 충돌을 검증한다. */
    @Test
    @DisplayName("이미 처리한 대화 버전으로 요청하면 HTTP 409이다")
    void staleRevision() throws Exception {
        // 다른 테스트가 만든 대화에 의존하지 않고 충돌에 필요한 상태를 직접 준비한다.
        var conversation = client.conversation();
        conversation.say("무료배송 상품 추천해줘");

        assertThat(conversation.requestStatus("무료배송 상품 추천해줘", "standard", 0)).isEqualTo(409);
    }

    /** 쿠키 하나로 서로 다른 대화를 동시에 실행해 결과가 섞이지 않는지 검증한다. */
    @Test
    @DisplayName("같은 세션의 동시 대화는 서로의 결과를 섞지 않는다")
    void concurrentConversations() throws Exception {
        // 쿠키를 먼저 발급받아 네 요청이 실제로 같은 세션에서 시작되도록 한다.
        client.conversation().say("무료배송 상품 추천해줘");
        List<String> scenarios = List.of("standard", "already-free", "empty", "standard");
        List<Callable<JsonNode>> requests = scenarios.stream()
                .<Callable<JsonNode>>map(scenario -> () -> client.conversation()
                        .send("무료배송 상품 추천해줘", scenario, false))
                .toList();

        try (var pool = Executors.newFixedThreadPool(4)) {
            var futures = pool.invokeAll(requests);
            var counts = new java.util.ArrayList<Integer>();
            for (var future : futures) {
                counts.add(ids(future.get()).size());
            }
            assertThat(counts).containsExactly(3, 0, 0, 3);
        }
    }

    /** 후속 요청에 필요한 최초 무료배송 대화만 준비한다. 검증은 각 테스트에서 수행한다. */
    private ChatClient.Conversation startFreeShippingConversation() throws Exception {
        var conversation = client.conversation();
        conversation.say("무료배송 상품을 추천해줘");
        return conversation;
    }

    private static void assertTaskId(JsonNode result, String expected) {
        assertThat(result.at("/task/taskId").asText()).isEqualTo(expected);
    }
}
