package com.example.ailearning.l12shopping;

import com.example.ailearning.testsupport.TestOutput;
import com.example.ailearning.testsupport.OpenAiStubServer;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantTextMessage;
import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantToolCallMessage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import java.net.*;
import java.net.http.*;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=",
        "learning.limits.max-tool-calls=4", "learning.limits.timeout-ms=1000"})
class ShoppingAgentLocalModelTest {
    private TestOutput output;

    @BeforeEach
    void 테스트_이름_준비(TestInfo info) {
        output = new TestOutput(info
                .getTestMethod()
                .orElseThrow()
                .getName());
    }

    static final OpenAiStubServer MODEL = createModelServer();
    @Value("${local.server.port}") int port;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.embedding.options.model", () -> "learning-embedding");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }
    @BeforeEach void reset() {
        MODEL
                .chat()
                .resetResponsesAndRequests();
        MODEL
                .chat()
                .setResponseFunction(this::script);
    }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void 상품과_배송비와_정책을_조회해_검증한_값으로_답한다() throws Exception {
        try (var alice = browser()) {
            var body = post(alice, "배송비 포함 30000원 음료 추천하고 미개봉 반품도 알려줘");
            assertThat(body.path("status").asString()).isEqualTo("VERIFIED");
            assertThat(body.at("/offers/0/product/id").asString()).isEqualTo("P01");
            assertThat(body.at("/offers/0/total").asInt()).isEqualTo(15000);
            assertThat(body.at("/sources/0/id").asString()).isEqualTo("return-sealed");
            assertThat(body.path("answer").asString()).contains("15000", "7일");
            assertThat(body.at("/trace/toolCalls").asInt()).isEqualTo(3);
            assertThat(body.path("storedMessages").asInt()).isEqualTo(2);
        }
    }
    @Test void 후속_질문에서_예산을_유지하고_결과_없는_조건도_기억한다() throws Exception {
        try (var alice = browser()) {
            // Given: 예산과 카테고리가 담긴 첫 대화 저장
            post(alice, "배송비 포함 30000원 음료 추천");

            // When: 예산은 유지한 채 카테고리만 변경
            var none = post(alice, "생활로만 바꿔");

            // Then: 변경 조건의 검색 결과가 없는지 확인
            assertThat(none.path("status").asString()).isEqualTo("NO_RESULTS");

            // When: 같은 대화에서 예산을 다시 변경
            var revised = post(alice, "예산을 40000원으로 바꿔");

            // Then: 이전 카테고리와 새 예산을 함께 적용한 결과·저장 이력 검증
            assertThat(revised.at("/criteria/category").asString()).isEqualTo("생활");
            assertThat(revised.at("/criteria/totalBudget").asInt()).isEqualTo(40000);
            assertThat(revised.at("/offers/0/product/id").asString()).isEqualTo("P03");
            assertThat(revised.path("storedMessages").asInt()).isEqualTo(6);
        }
    }
    @Test void 배송비를_더하면_모든_상품이_예산을_초과할_수_있다() throws Exception {
        // Given: 배송비 포함 예산을 넘는 상품만 반환하는 모델 흐름 설정
        MODEL
                .chat()
                .setResponseFunction(request -> {
                    if (isIntent(request)) return intent("SHOPPING", "음료", "13000");
                    int count = toolResults(request)
                            .size();
                    if (count == 0) return createAssistantToolCallMessage("s", "searchProducts", "{}");
                    if (count == 1) return createAssistantToolCallMessage("q", "calculateShipping", "{\"productId\":\"P01\"}");
                    return selection(List.of(), List.of());
                });
        try (var alice = browser()) {
            // When: 배송비 포함 예산으로 상담 요청
            var body = post(alice, "배송비 포함 13000원 음료 추천");

            // Then: 추천 결과 없음과 대화 저장 상태 검증
            assertThat(body.path("status").asString()).isEqualTo("NO_RESULTS");
            assertThat(body.path("storedMessages").asInt()).isEqualTo(2);
        }
    }
    @Test void 예산이_없으면_도구_사용_전에_추가_질문을_한다() throws Exception {
        try (var alice = browser()) {
            var body = post(alice, "선물 추천");
            assertThat(body.path("status").asString()).isEqualTo("NEEDS_INPUT");
            assertThat(body.at("/trace/toolCalls").asInt()).isZero();
            assertThat(body.path("storedMessages").asInt()).isEqualTo(2);
        }
    }
    @Test void 정책_질문에는_상품_도구를_제공하지_않고_없는_근거는_만들지_않는다() throws Exception {
        try (var alice = browser()) {
            var body = post(alice, "미개봉 반품 기간");
            assertThat(body.path("status").asString()).isEqualTo("VERIFIED");
            assertThat(MODEL.chat().getRequests().get(1).path("tools").toString()).contains("searchPolicy").doesNotContain("searchProducts");
            var unknown = post(alice, "우주 여행 정책");
            assertThat(unknown.path("status").asString()).isEqualTo("NO_CONTEXT");
        }
    }
    @Test void 확인하지_않은_ID와_예산_초과_선택은_거부하고_저장하지_않는다() throws Exception {
        try (var alice = browser()) {
            // Given: 배송비 포함 예산을 초과한 상품을 선택하도록 모델 설정
            MODEL
                    .chat()
                    .setResponseFunction(request -> {
                        if (isIntent(request)) return intent("SHOPPING", "음료", "30000");
                        if (toolResults(request)
                                .isEmpty()) return createAssistantToolCallMessage("q", "calculateShipping", "{\"productId\":\"P02\"}");
                        return selection(List.of("P02"), List.of());
                    });

            // When: 예산 초과 선택을 포함한 상담 실행
            var expensive = post(alice, "30000원 음료 추천");

            // Then: 선택 검증에 실패하고 대화가 저장되지 않는지 확인
            assertThat(expensive.path("status").asString()).isEqualTo("VERIFY_FAILED");
            assertThat(expensive.path("storedMessages").asInt()).isZero();

            // Given: 존재하지 않는 정책 출처를 선택하도록 모델 응답 변경
            MODEL
                    .chat()
                    .setResponseFunction(request -> isIntent(request) ? intent("POLICY", null, "null") : selection(List.of(), List.of("invented")));

            // When: 정책 상담 실행
            var unknownSource = post(alice, "반품");

            // Then: 확인하지 않은 출처를 선택한 답변 거부
            assertThat(unknownSource.path("status").asString()).isEqualTo("VERIFY_FAILED");
        }
    }
    @Test void 실패해도_기존_대화를_유지하고_사용자별로_격리한다() throws Exception {
        try (var alice = browser(); var bob = browser()) {
            // Given: Alice와 Bob의 대화를 저장하고 세션 격리 확인
            post(alice, "30000원 음료 추천");
            post(bob, "선물 추천");
            assertThat(MODEL.chat().getRequests().get(MODEL.chat().getRequests().size()-1).path("messages").toString()).doesNotContain("30000원");

            // Given: 다음 모델 호출은 HTTP 429로 실패하도록 변경
            MODEL
                    .chat()
                    .setHttpStatus(429);

            // When: Alice의 후속 상담 요청
            var failed = post(alice, "조건을 바꿔줘");

            // Then: 실패한 대화는 추가하지 않고 기존 대화 유지
            assertThat(failed.path("status").asString()).isEqualTo("FAILED");
            assertThat(failed.path("storedMessages").asInt()).isEqualTo(2);

            // When: Alice 대화 삭제
            output.printHttpResponse(alice.send(HttpRequest.newBuilder(uri("/api/counsel/demo"))
                                    .DELETE()
                                    .build(), HttpResponse.BodyHandlers.ofString()));

            // Then: Alice만 삭제되고 Bob의 대화는 유지되는지 검증
            assertThat(history(alice).size()).isZero();
            assertThat(history(bob).size()).isEqualTo(2);
        }
    }
    @Test void 도구_호출_상한에_도달하면_미완성_대화를_저장하지_않는다() throws Exception {
        // Given: 상품 검색 도구를 계속 요청하는 가짜 모델 설정
        MODEL
                .chat()
                .setResponseFunction(request -> isIntent(request) ? intent("SHOPPING", "음료", "30000")
                : createAssistantToolCallMessage("repeat", "searchProducts", "{}"));
        try (var alice = browser()) {
            // When: 상담 API 실행
            var body = post(alice, "30000원 음료 추천");

            // Then: 도구 상한에서 중단하고 미완성 대화를 저장하지 않았는지 검증
            assertThat(body.path("status").asString()).isEqualTo("TOOL_LIMIT");
            assertThat(body.at("/trace/toolCalls").asInt()).isEqualTo(4);
            assertThat(body.path("storedMessages").asInt()).isZero();
        }
    }
    @Test void 시간_초과_뒤에_도착한_응답은_저장하지_않는다() throws Exception {
        // Given: 응답이 시간 제한보다 늦게 오도록 설정
        MODEL
                .chat()
                .setResponseDelay(java.time.Duration.ofMillis(1500));
        try (var alice = browser()) {
            // When: 상담 API 실행
            var body = post(alice, "선물 추천");

            // Then: 시간 초과 이후에도 늦은 응답이 저장되지 않는지 검증
            assertThat(body.path("status").asString()).isEqualTo("TIMEOUT");
            Thread.sleep(800);
            assertThat(history(alice).size()).isZero();
            assertThat(MODEL.chat().getRequests()).hasSize(1);
        }
    }

    // 고정 시나리오만 처리하는 테스트 대역이다. 한국어 이해 성능을 평가하지 않는다.
    ObjectNode script(JsonNode request) {
        List<JsonNode> users = new ArrayList<>();
        for (JsonNode message : request.path("messages")) if (message
                .path("role")
                .asString()
                .equals("user")) users.add(message);
        String latest = users
                .get(users.size()-1)
                .path("content")
                .asString();
        String all = request
                .path("messages")
                .toString();
        if (isIntent(request)) {
            String task = latest.contains("반품") && !latest.contains("추천") || latest.contains("우주") ? "POLICY" : "SHOPPING";
            String category = latest.contains("생활") || all.contains("생활로만") ? "생활" : all.contains("음료") ? "음료" : null;
            return intent(task, category, all.contains("40000") ? "40000" : all.contains("30000") ? "30000" : "null");
        }
        var results = toolResults(request);
        boolean policyOnly = latest.contains("task=POLICY");
        if (policyOnly) {
            if (results.isEmpty()) return createAssistantToolCallMessage("p", "searchPolicy", all.contains("우주") ? "{\"question\":\"우주 여행\"}" : "{\"question\":\"미개봉 반품\"}");
            return selection(List.of(), results
                            .getLast()
                            .contains("return-sealed") ? List.of("return-sealed") : List.of());
        }
        if (results.isEmpty()) return createAssistantToolCallMessage("s", "searchProducts", "{}");
        String searchResult = results.getFirst();
        String product = searchResult.contains("P01") ? "P01" : searchResult.contains("P03") ? "P03" : null;
        if (product == null) return selection(List.of(), List.of());
        if (results.size()==1) return createAssistantToolCallMessage("q", "calculateShipping", "{\"productId\":\""+product+"\"}");
        boolean policy = all.contains("반품");
        if (policy && results.size()==2) return createAssistantToolCallMessage("p", "searchPolicy", "{\"question\":\"미개봉 반품\"}");
        return selection(List.of(product), policy ? List.of("return-sealed") : List.of());
    }
    boolean isIntent(JsonNode request) { return request
            .path("messages")
            .get(0)
            .path("content")
            .asString()
            .contains("TASK:INTENT"); }
    List<String> toolResults(JsonNode request) {
        var results = new ArrayList<String>();
        for (var message : request.path("messages")) if (message
                .path("role")
                .asString()
                .equals("tool")) results.add(message
                                .path("content")
                                .asString());
        return results;
    }
    ObjectNode intent(String task, String category, String budget) {
        return createAssistantTextMessage("{\"task\":\""+task+"\",\"category\":"+(category==null?"null":"\""+category+"\"")+",\"totalBudget\":"+budget+"}");
    }
    ObjectNode selection(List<String> products, List<String> sources) {
        var object = OpenAiStubServer.JSON.createObjectNode();
        var productIds = object.putArray("productIds"); products.forEach(productIds::add);
        var sourceIds = object.putArray("sourceIds"); sources.forEach(sourceIds::add);
        return createAssistantTextMessage(object.toString());
    }
    HttpClient browser() { return HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build(); }
    URI uri(String path) { return URI.create("http://127.0.0.1:"+port+path); }
    JsonNode post(HttpClient browser, String question) throws Exception {
        var response = output.printHttpResponse(browser.send(HttpRequest.newBuilder(uri("/api/counsel/demo"))
                                .header("Content-Type", "text/plain; charset=UTF-8")
                                .POST(HttpRequest.BodyPublishers.ofString(question))
                                .build(), HttpResponse.BodyHandlers.ofString()));
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return OpenAiStubServer.JSON.readTree(response.body());
    }
    JsonNode history(HttpClient browser) throws Exception {
        return OpenAiStubServer.JSON.readTree(output
                        .printHttpResponse(browser.send(HttpRequest.newBuilder(uri("/api/counsel/demo"))
                                        .GET()
                                        .build(),HttpResponse.BodyHandlers.ofString()))
                        .body());
    }

    private static OpenAiStubServer createModelServer() {
        var server = new OpenAiStubServer();
        server
                .embeddings()
                .setVectorFunction(text -> {
                    if (text.contains("배송") || text.contains("운송")) return new float[]{1, 0, 0};
                    if (text.contains("반품") || text.contains("개봉")) return new float[]{0, 1, 0};
                    return new float[]{0, 0, 1};
                });
        server
                .chat()
                .setTokenUsage(10, 5);
        return server;
    }
}
