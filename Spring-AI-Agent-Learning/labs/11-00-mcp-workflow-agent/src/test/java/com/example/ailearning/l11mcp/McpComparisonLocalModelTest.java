package com.example.ailearning.l11mcp;

import com.example.ailearning.l11mcp.tools.ShoppingMcpGateway;

import com.example.ailearning.testsupport.OpenAiStubServer;
import com.example.ailearning.testsupport.TestNamePrinter;
import com.example.ailearning.testsupport.TestOutput;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.*;
import java.util.List;
import java.util.Map;
import static com.example.ailearning.testsupport.OpenAiChatStub.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=", "spring.ai.mcp.server.enabled=false", "learning.mcp.max-tool-calls=4"})
class McpComparisonLocalModelTest {
    private static final OpenAiStubServer MODEL = new OpenAiStubServer();
    private static final McpTestServer MCP = new McpTestServer();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final TestOutput output = new TestOutput();
    @Value("${local.server.port}") int port;
    @Autowired ShoppingMcpGateway shoppingMcpGateway;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
        registry.add("spring.ai.mcp.client.streamable-http.connections.shopping.url", MCP::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL
                .chat()
                .resetResponsesAndRequests();
    }

    @AfterAll
    static void 서버_종료() {
        MODEL.close();
        MCP.close();
    }

    @Test
    void 모델_없이_실제_MCP의_도구_목록과_배송_정책을_조회한다() {
        List<String> names = shoppingMcpGateway.listToolNames();
        JsonNode policy = shoppingMcpGateway.callTool("getShippingPolicy", Map.of());
        output.printValue("발견한 MCP 도구", names);
        output.printValue("배송 정책", policy);
        assertThat(names).contains("searchProducts", "quoteCart", "getShippingPolicy", "getCart");
        assertThat(policy.path("threshold").asLong()).isEqualTo(50000);
        assertThat(policy.path("shippingFee").asLong()).isEqualTo(3000);
        assertThat(MODEL.chat().getRequests()).isEmpty();
    }

    @Test
    void Workflow는_검색된_두_상품의_ID를_모두_계산에_전달한다() throws Exception {
        // Given: 모델은 사용자 질문에서 검색 조건만 추출한다.
        MODEL
                .chat()
                .setResponseText("{\"category\":\"음료\",\"maxPrice\":15000,\"totalBudget\":30000}");
        // When: Java가 검색 다음 계산을 실행한다.
        JsonNode result = post("workflow", "음료 15000원 이하 모두 구매, 배송비 포함 3만원 이내");
        // Then: MCP 결과와 다음 도구에 전달한 ID 전체를 확인한다.
        assertThat(result.path("status").asString()).isEqualTo("COMPLETED");
        assertThat(result.path("toolCalls").size()).isEqualTo(2);
        assertThat(result.at("/toolCalls/0/name").asString()).isEqualTo("searchProducts");
        assertThat(result.at("/toolCalls/1/arguments/additionalProductIds").toString()).isEqualTo("[\"P004\",\"P005\"]");
        assertThat(result.at("/toolCalls/1/result/total").asLong()).isEqualTo(25400);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        assertThat(MODEL.chat().getRequests().getFirst().path("tools").isMissingNode()).isTrue();
    }

    @Test
    void Agent가_선택한_원격_도구의_결과가_다음_모델_입력에_포함된다() throws Exception {
        MODEL
                .chat()
                .setResponseFunction(request -> createShoppingToolSequenceResponse(request, 30000, false));
        JsonNode result = post("agent", "15000원 이하 음료 모두 한 개씩, 총액 알려줘");
        assertThat(result.path("status").asString()).isEqualTo("ANSWERED");
        assertThat(result.at("/toolCalls/1/result/total").asLong()).isEqualTo(25400);
        assertThat(MODEL.chat().getRequests()).hasSize(3);
        String tools = MODEL.chat().getRequests().getFirst().path("tools").toString();
        assertThat(tools).contains("searchProducts", "quoteCart").doesNotContain("getPoints", "getCoupons");
        assertThat(MODEL.chat().getRequests().getLast().path("messages").toString()).contains("25400", "tool");
    }

    @Test
    void 하이브리드는_검색_조건을_고정하고_잘못된_답변_대신_실제_총액을_사용한다() throws Exception {
        // Given: 모델이 검색 가격을 넓히고 최종 금액도 잘못 말한다.
        MODEL
                .chat()
                .setResponseFunction(request -> createShoppingToolSequenceResponse(request, 30000, false));
        JsonNode result = post("hybrid", "음료 15000원 이하 모두, 배송비 포함 3만원 이내");
        assertThat(result.path("status").asString()).isEqualTo("VERIFIED");
        assertThat(result.at("/toolCalls/0/arguments/query/maxPrice").asInt()).isEqualTo(15000);
        assertThat(result.path("answer").asString()).contains("25400").doesNotContain("1원");
        assertThat(MODEL.chat().getRequests()).hasSize(4);
    }

    @Test
    void 하이브리드는_상품의_일부만_계산하는_요청을_거부한다() throws Exception {
        MODEL
                .chat()
                .setResponseFunction(request -> createShoppingToolSequenceResponse(request, 30000, true));
        HttpResponse<String> response = output.printHttpResponse(request("hybrid", "음료 15000원 이하 모두 구매"));
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(MODEL.chat().getRequests()).hasSize(3);
    }

    @Test
    void 하이브리드는_실제_장바구니_총액으로_예산을_검증한다() throws Exception {
        MODEL
                .chat()
                .setResponseFunction(request -> createShoppingToolSequenceResponse(request, 20000, false));
        JsonNode result = post("hybrid", "음료 15000원 이하 모두, 총예산 2만원");
        assertThat(result.path("status").asString()).isEqualTo("OVER_BUDGET");
        assertThat(result.path("answer").asString()).contains("25400");
    }

    @Test
    void 검색_조건이_부족하거나_상품이_없으면_후속_호출을_생략한다() throws Exception {
        MODEL
                .chat()
                .setResponseText("{\"category\":null,\"maxPrice\":null,\"totalBudget\":null}");
        assertThat(post("workflow", "추천해줘").path("status").asString()).isEqualTo("NEEDS_INPUT");
        MODEL
                .chat()
                .setResponseText("{\"category\":\"음료\",\"maxPrice\":1,\"totalBudget\":null}");
        JsonNode result = post("workflow", "1원 이하 음료");
        assertThat(result.path("status").asString()).isEqualTo("NO_RESULTS");
        assertThat(result.path("toolCalls").size()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void 하이브리드는_빈_검색_뒤_계산_여부와_무관하게_결과_없음을_반환한다(boolean calculateEmptyCart) throws Exception {
        // 실제 MCP 검색은 빈 배열을 반환한다. 모델은 계산을 생략하거나 빈 장바구니를 계산한다.
        MODEL
                .chat()
                .setResponseFunction(request -> {
            if (request.path("messages").toString().contains("TASK:MCP_EXTRACT")) {
                return createAssistantTextMessage("{\"category\":\"음료\",\"maxPrice\":1,\"totalBudget\":30000}");
            }
            long results = java.util.stream.StreamSupport
                    .stream(request.path("messages").spliterator(), false)
                    .filter(message -> message.path("role").asString().equals("tool"))
                    .count();
            if (results == 0) {
                return createAssistantToolCallMessage("search", "searchProducts", "{\"query\":{\"category\":\"음료\",\"maxPrice\":1}}");
            }
            if (results == 1 && calculateEmptyCart) {
                return createAssistantToolCallMessage("quote", "quoteCart", "{\"scenario\":\"empty\",\"additionalProductIds\":[]}");
            }
            return createAssistantTextMessage("총액은 0원입니다.");
        });

        JsonNode result = post("hybrid", "1원 이하 음료 모두 구매");

        assertThat(result.path("status").asString()).isEqualTo("NO_RESULTS");
        assertThat(result.path("answer").asString()).doesNotContain("0원");
        assertThat(result.path("toolCalls").size()).isEqualTo(calculateEmptyCart ? 2 : 1);
        assertThat(result.at("/toolCalls/0/result").isEmpty()).isTrue();
    }

    @Test
    void 인사는_도구를_생략하고_반복_호출은_상한에서_중단한다() throws Exception {
        MODEL
                .chat()
                .setResponseText("안녕하세요!");
        assertThat(post("agent", "안녕").path("toolCalls").isEmpty()).isTrue();
        MODEL
                .chat()
                .setResponseFunction(request -> createAssistantToolCallMessage("repeat", "searchProducts",
                "{\"query\":{\"category\":\"음료\",\"maxPrice\":15000}}"));
        HttpResponse<String> response = output.printHttpResponse(request("agent", "계속 검색"));
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(MODEL.chat().getRequests()).hasSize(6);
    }

    @Test
    void 모델이_계산을_생략하면_하이브리드는_검증_완료로_표시하지_않는다() throws Exception {
        MODEL
                .chat()
                .setResponseFunction(request -> request.path("messages").toString().contains("TASK:MCP_EXTRACT")
                        ? createAssistantTextMessage("{\"category\":\"음료\",\"maxPrice\":15000,\"totalBudget\":30000}")
                        : createAssistantTextMessage("계산 없이 답하겠습니다."));
        JsonNode result = post("hybrid", "음료 15000원 이하 모두 구매");
        assertThat(result.path("status").asString()).isEqualTo("UNVERIFIED");
        assertThat(result.path("toolCalls").isEmpty()).isTrue();
    }

    @Test
    void 실제_MCP_서버의_도구_실패가_성공_결과로_변환되지_않는다() {
        RuntimeException error = catchThrowableOfType(RuntimeException.class,
                () -> shoppingMcpGateway.callTool("quoteCart",
                        Map.of("scenario", "empty", "additionalProductIds", List.of("unknown"))));
        output.printValue("MCP 도구 실패", error);
        assertThat(error).isNotNull();
        assertThat(MODEL.chat().getRequests()).isEmpty();
    }

    @Test
    void 공백_질문은_모델_호출_전에_거부한다() throws Exception {
        HttpResponse<String> response = output.printHttpResponse(request("agent", "   "));
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(MODEL.chat().getRequests()).isEmpty();
    }

    ObjectNode createShoppingToolSequenceResponse(JsonNode request, long budget, boolean omitProduct) {
        if (request.path("messages").toString().contains("TASK:MCP_EXTRACT")) {
            return createAssistantTextMessage("{\"category\":\"음료\",\"maxPrice\":15000,\"totalBudget\":" + budget + "}");
        }
        int results = 0;
        for (JsonNode message : request.path("messages")) {
            if (message.path("role").asString().equals("tool")) results++;
        }
        return switch (results) {
            case 0 -> createAssistantToolCallMessage("search", "searchProducts",
                    "{\"query\":{\"category\":\"음료\",\"maxPrice\":99999,\"shippingGroups\":[\"MALL\"]}}");
            case 1 -> createAssistantToolCallMessage("quote", "quoteCart",
                    "{\"scenario\":\"empty\",\"additionalProductIds\":"
                            + (omitProduct ? "[\"P004\"]" : "[\"P004\",\"P005\"]") + "}");
            default -> createAssistantTextMessage("총액은 1원입니다.");
        };
    }

    HttpResponse<String> request(String mode, String question) throws Exception {
        return httpClient.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/mcp/" + mode))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(question))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    JsonNode post(String mode, String question) throws Exception {
        HttpResponse<String> response = output.printHttpResponse(request(mode, question));
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return OpenAiStubServer.JSON.readTree(response.body());
    }
}
