# 05-2. Java 메서드를 도구로 제공하기

30~45분을 기준으로 한 번에 한 가지 흐름만 읽습니다. 단원 전체 시간은 목차의 기존 예상 시간에 포함됩니다.

**이번 목표:** 도구 설명 등록과 실제 메서드 실행을 구분합니다.

**코드 읽기 안내:** @Tool은 모델에게 메서드의 사용법을 설명합니다. 메서드 본문을 외부 모델 서버로 보내는 것이 아닙니다.

## 같은 기능에 도구 설명 붙이기

[ShoppingTools.java](../../../labs/05-tool-calling/src/main/java/com/example/ailearning/l05tools/ShoppingTools.java)의 전체 코드입니다.

```java
package com.example.ailearning.l05tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ShoppingTools {
    private final ProductCatalog catalog;

    public ShoppingTools(ProductCatalog catalog) { this.catalog = catalog; }

    @Tool(description = "가상 상품을 검색합니다. category는 음료/생활, maxPrice는 상품 가격 상한입니다. 생략하면 해당 조건은 적용하지 않습니다.")
    public List<Product> searchProducts(
            @ToolParam(required = false) String category,
            @ToolParam(required = false) Integer maxPrice) {
        return catalog.search(category, maxPrice);
    }

    @Tool(description = "존재하는 상품 ID의 국내 기본 배송비와 총액을 조회합니다. 가격을 직접 입력하지 말고 검색 결과의 상품 ID를 사용하세요.")
    public Offer calculateShipping(String productId) { return catalog.quote(productId); }
}
```

새 업무 규칙을 어노테이션 안에 구현한 것이 아닙니다. 원래 Java 서비스로 위임합니다.
`required=false`는 조건 생략을 허용하고, null이면 해당 필터를 적용하지 않습니다.
도구 설명은 모델의 선택을 돕고, Java 검증은 실제 입력과 계산을 보호합니다.

## 모델과 연결하기

[ToolChatService.java](../../../labs/05-tool-calling/src/main/java/com/example/ailearning/l05tools/ToolChatService.java)의 전체 코드입니다.

```java
package com.example.ailearning.l05tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ToolChatService {
    private final ChatClient client;
    private final ShoppingTools tools;

    public ToolChatService(ChatClient.Builder builder, ShoppingTools tools) {
        this.client = builder.build();
        this.tools = tools;
    }

    public String ask(String question) {
        return client.prompt()
                .system("""
                        가상 상품 상담을 합니다. 가격과 배송비는 도구 결과만 사용하세요.
                        필요에 따라 상품 검색과 배송비 조회 도구를 선택하세요.
                        배송비를 물으면 상품 가격과 구분해서 설명하세요.
                        상품 조회가 필요 없는 인사에는 도구 없이 답할 수 있습니다.
                        """)
                .user(question)
                .tools(tools)
                .call().content();
    }
}
```

03장과 비교해 핵심으로 추가된 코드는 `.tools(tools)`입니다.
여기서 `tools`는 ShoppingTools 객체이고, Spring AI는 `@Tool` 메서드에서 도구 설명과 인자 구조를 만듭니다.
모델은 `{"productId":"P02"}` 같은 인자를 요청합니다. Java 객체를 모델 서버로 보내서 실행시키는 방식이 아닙니다.


## 여기서 멈춰 확인하기

코드를 닫고 “이번 입력 → 새로 하는 처리 → 확인할 결과”를 한 문장으로 적어 보세요.
설명하기 어렵다면 위 핵심 메서드만 다시 읽습니다. 테스트용 HTTP 서버 구현을 이해해야 다음으로 갈 수 있는 것은 아닙니다.

[이전](01-java.md) · [다음 소수업](03-loop.md) · [학습 방법](../../learning-guide.md)
