package com.example.shopping.agent.capabilities;

import java.util.List;
import static com.example.shopping.agent.ports.ShoppingContracts.*;

/** Actual variation point: bounded model-led tool selection or direct read-only search. */
public interface CatalogSearchStrategy {
    List<Product> search(String message, ProductQuery query, Catalog catalog);
    String name();
    class InvalidPlan extends RuntimeException {
        public InvalidPlan() { super("상품 탐색 계획/결과 검증 실패"); }
    }
}
