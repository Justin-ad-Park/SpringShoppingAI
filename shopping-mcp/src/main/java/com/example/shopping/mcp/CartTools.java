package com.example.shopping.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import static com.example.shopping.mcp.ShoppingModels.*;

@Service
public class CartTools {
    private final MockShoppingStore store;
    public CartTools(MockShoppingStore store) { this.store = store; }
    @Tool(description = "고정 데모 고객 장바구니 조회. scenario는 standard/already-free/empty/no-match. 합계는 서버가 수량을 반영해 계산합니다.")
    public Cart getCart(String scenario) { return store.cart(scenario); }
}
