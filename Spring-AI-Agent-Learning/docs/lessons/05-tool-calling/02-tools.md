# 05-2. Java 메서드를 도구로 제공하기

**실행 코드·설정·테스트:** [labs/05-00-tool-calling](../../../labs/05-00-tool-calling/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 도구 설명 등록과 실제 메서드 실행을 구분합니다.

**코드 읽기 안내:** @Tool은 모델에게 메서드의 사용법을 설명합니다. 메서드 본문을 외부 모델 서버로 보내는 것이 아닙니다.

## 같은 기능에 도구 설명 붙이기

`@Tool`은 메서드를 도구로 설명하고, `@ToolParam`은 인자의 설명이나 생략 가능 여부를 정합니다.
어노테이션을 붙였다고 모든 모델 호출에서 자동으로 노출되지는 않습니다. 이번 요청의 `.tools(tools)`에 명시적으로 등록합니다.

[ShoppingTools.java](../../../labs/05-00-tool-calling/src/main/java/com/example/ailearning/l05tools/ShoppingTools.java)의 전체 코드입니다.

```java
package com.example.ailearning.l05tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ShoppingTools {
    private final ProductCatalog productCatalog;

    public ShoppingTools(ProductCatalog productCatalog) { this.productCatalog = productCatalog; }

    @Tool(description = "가상 상품을 검색합니다. category는 음료/생활, maxPrice는 상품 가격 상한입니다. 생략하면 해당 조건은 적용하지 않습니다.")
    public List<Product> searchProducts(
            @ToolParam(required = false) String category,
            @ToolParam(required = false) Integer maxPrice) {
        return productCatalog.search(category, maxPrice);
    }

    @Tool(description = "존재하는 상품 ID의 국내 기본 배송비와 총액을 조회합니다. 가격을 직접 입력하지 말고 검색 결과의 상품 ID를 사용하세요.")
    public ProductPriceDetails calculateShipping(String productId) { return productCatalog.calculatePriceDetails(productId); }
}
```

새 업무 규칙을 어노테이션 안에 구현한 것이 아닙니다. 원래 Java 서비스로 위임합니다.
`required=false`는 조건 생략을 허용하고, null이면 해당 필터를 적용하지 않습니다.
도구 설명은 모델의 선택을 돕고, Java 검증은 실제 입력과 계산을 보호합니다.

## 모델과 연결하기

[ToolChatService.java](../../../labs/05-00-tool-calling/src/main/java/com/example/ailearning/l05tools/ToolChatService.java)의 전체 코드입니다.

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
        return client
                .prompt()
                .system("""
                        가상 상품 상담을 합니다. 가격과 배송비는 도구 결과만 사용하세요.
                        필요에 따라 상품 검색과 배송비 조회 도구를 선택하세요.
                        배송비를 물으면 상품 가격과 구분해서 설명하세요.
                        상품 조회가 필요 없는 요청에는 도구 없이 답할 수 있습니다.
                        """)
                .user(question)
                .tools(tools)
                .call()
                .content();
    }
}
```

03장과 비교해 핵심으로 추가된 코드는 `.tools(tools)`입니다.
여기서 `tools`는 ShoppingTools 객체이고, Spring AI는 `@Tool` 메서드에서 도구 설명과 인자 구조를 만듭니다.
모델은 `{"productId":"P02"}` 같은 인자를 요청합니다. Java 객체를 모델 서버로 보내서 실행시키는 방식이 아닙니다.


## 여기서 멈춰 확인하기

`@Tool`로 설명한 메서드가 모델 요청에 제공되는 위치는 어디인가요?

[이전](01-java.md) · [다음](03-loop.md) · [단원 목차](../05-tool-calling.md) · [학습 방법](../../learning-guide.md)
