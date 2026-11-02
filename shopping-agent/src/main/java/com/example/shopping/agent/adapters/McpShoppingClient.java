package com.example.shopping.agent.adapters;

import java.util.*;
import tools.jackson.core.type.TypeReference;
import com.example.shopping.agent.execution.McpSession;
import static com.example.shopping.agent.ports.ShoppingContracts.*;

/** One adapter implements small domain-facing ports; tasks depend only on their port contracts. */
public class McpShoppingClient implements Catalog, CartPricing, Benefits {
    private final McpSession session;
    private final String scenario;
    public McpShoppingClient(McpSession session, String scenario) { this.session = session; this.scenario = scenario; }
    public List<Product> search(ProductQuery query) { return session.call("searchProducts", Map.of("query", query), new TypeReference<>() {}); }
    public Cart cart() { return session.call("getCart", Map.of("scenario", scenario), new TypeReference<>() {}); }
    public Policy policy() { return session.call("getShippingPolicy", Map.of(), new TypeReference<>() {}); }
    public Quote quote(List<String> ids) { return session.call("quoteCart", Map.of("scenario", scenario, "additionalProductIds", ids), new TypeReference<>() {}); }
    public Map<String,Object> coupons() { return session.call("getCoupons", Map.of(), new TypeReference<>() {}); }
    public Map<String,Object> points() { return session.call("getPoints", Map.of(), new TypeReference<>() {}); }
}
