package com.example.shopping.agent.capabilities;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.function.FunctionToolCallback;
import static com.example.shopping.agent.ports.ShoppingContracts.*;

/** Exposes only catalog search to this model, with server-validated constraints bound to it. */
public class LlmCatalogSearch implements CatalogSearchStrategy {
    private final ChatClient client;
    private final ObjectMapper mapper;
    public record Selection(List<String> productIds) {}
    public LlmCatalogSearch(ChatClient client, ObjectMapper mapper) { this.client = client; this.mapper = mapper; }
    public List<Product> search(String message, ProductQuery query, Catalog catalog) {
        AtomicReference<List<Product>> observed = new AtomicReference<>();
        var tool = FunctionToolCallback.builder("searchProducts", (ProductQuery ignored) -> {
            List<Product> products = catalog.search(query);
            observed.set(products);
            return products;
        }).description("범용 상품 검색. 입력은 이미 검증된 검색 조건에 서버가 바인딩합니다. 가격·카테고리를 임의 변경하지 않습니다.")
            .inputType(ProductQuery.class).build();
        String content = client.prompt().system("""
            일반 상품 탐색을 수행하세요. searchProducts를 호출한 뒤 실제 반환된 상품 ID만 최대 5개 선택하세요.
            장바구니나 무료배송 조건을 추가하지 마세요. 검색 조건은 서버가 확정합니다.
            최종 JSON은 {"productIds":["상품ID"]}입니다. 결과가 있으면 적합한 상품을 최소 1개 선택하세요.
            """).user(message + "\n확정 검색 조건: " + mapper.valueToTree(query))
            .tools(tool).call().content();
        Selection result;
        try { result = mapper.readValue(content == null ? "" : content.strip().replaceAll("^```(?:json)?\\s*|\\s*```$", ""), Selection.class); }
        catch (tools.jackson.core.JacksonException e) { throw new InvalidPlan(); }
        List<Product> found = observed.get();
        if (found == null || result == null || result.productIds() == null || result.productIds().size() > 5
                || (!found.isEmpty() && result.productIds().isEmpty())) throw new InvalidPlan();
        Map<String,Product> byId = new HashMap<>();
        found.forEach(p -> byId.put(p.id(), p));
        if (result.productIds().stream().anyMatch(id -> !byId.containsKey(id))) throw new InvalidPlan();
        return result.productIds().stream().distinct().map(byId::get).toList();
    }
    public String name() { return "AGENT"; }
}
