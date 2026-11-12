# 05-1. 모델 없이 상품 서비스를 호출하기

**실행 코드·설정·테스트:** [labs/05-00-tool-calling](../../../labs/05-00-tool-calling/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 가격과 배송비의 출처를 일반 Java 코드에서 찾습니다.

**코드 읽기 안내:** 먼저 Product와 ProductPriceDetail를 읽습니다. 모델을 연결하기 전에 상품 P01의 총액 15000원을 손으로 계산하세요.

## 왜 도구가 필요한가?

모델은 우리 상품 데이터에 직접 접근하지 못합니다. “허브티 가격이 얼마야?”라는 질문만으로 실제 가격을 보장할 수 없습니다.
먼저 Java 상품 서비스를 만들고, 모델에게 이 서비스를 사용할 수 있는 도구 설명을 전달합니다.
모델은 도구 이름과 인자를 응답하고, Spring AI가 Java 메서드를 실행한 뒤 결과를 모델에 다시 보냅니다.

**도구(tool)**는 애플리케이션이 허용한 실행 가능한 기능입니다.

## 먼저 일반 Java 서비스를 읽기

가상 상품 데이터와 배송 정책을 사용하는 Java 서비스부터 읽습니다.
상품 한 개씩 비교하며, 국내 기본 배송비는 상품 가격 3만원 이상이면 0원, 미만이면 3000원입니다.

| ID | 상품 | 카테고리 | 상품 가격 | 배송비 포함 총액 |
|---|---|---|---:|---:|
| P01 | 허브티 | 음료 | 12000 | 15000 |
| P02 | 커피 | 음료 | 28000 | 31000 |
| P03 | 텀블러 | 생활 | 35000 | 35000 |

```mermaid
classDiagram
    class ToolController
    class ProductCatalog {
        +search(category, maxPrice) List
        +calculatePriceDetail(productId) ProductPriceDetail
    }
    class Product
    class ProductPriceDetail
    ToolController --> ProductCatalog : 직접 호출 경로
    ProductCatalog --> Product : 상품 조회
    ProductCatalog --> ProductPriceDetail : 배송비 포함 총액 반환
    ProductPriceDetail --> Product : 상품 정보 포함
```

[ProductCatalog.java](../../../labs/05-00-tool-calling/src/main/java/com/example/ailearning/l05tools/domain/ProductCatalog.java)의 전체 코드입니다.

```java
package com.example.ailearning.l05tools.domain;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;

@Service
public class ProductCatalog {
    private final List<Product> products = List.of(
            new Product("P01", "허브티", "음료", 12000),
            new Product("P02", "커피", "음료", 28000),
            new Product("P03", "텀블러", "생활", 35000));

    public List<Product> search(String category, Integer maxPrice) {
        if (category != null && !Set.of("음료", "생활")
                .contains(category)) {
            throw new IllegalArgumentException("category는 음료/생활 또는 null입니다.");
        }
        if (maxPrice != null && maxPrice < 0) throw new IllegalArgumentException("가격은 0 이상입니다.");
        return products
                .stream()
                .filter(product -> category == null || product
                        .category()
                        .equals(category))
                .filter(product -> maxPrice == null || product.price() <= maxPrice)
                .toList();
    }

    public ProductPriceDetail calculatePriceDetail(String productId) {
        Product product = products
                .stream()
                .filter(item -> item
                        .id()
                        .equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        int shippingFee = product.price() >= 30000 ? 0 : 3000;
        return new ProductPriceDetail(product, shippingFee, Math.addExact(product.price(), shippingFee));
    }
}
```

`search()`는 일반 Java 필터입니다. `calculatePriceDetail()`는 모델이 보내는 가격을 받지 않고 ID로 조회한 가격으로 계산합니다.
[Product](../../../labs/05-00-tool-calling/src/main/java/com/example/ailearning/l05tools/domain/Product.java)와 [ProductPriceDetail](../../../labs/05-00-tool-calling/src/main/java/com/example/ailearning/l05tools/domain/ProductPriceDetail.java)는 04장에서 배운 record입니다.


## 상품 기능만 테스트하기

[ProductCatalogUnitTest](../../../labs/05-00-tool-calling/src/test/java/com/example/ailearning/l05tools/domain/ProductCatalogUnitTest.java)는 `new ProductCatalog()`로 객체를 만들고 검색 조건·상품 ID 검증과 무료 배송을 검사합니다. Spring이나 모델 서버를 띄울 필요가 없습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:05-00-tool-calling:test --tests com.example.ailearning.l05tools.domain.ProductCatalogUnitTest
```

도구 호출과 HTTP 연결은 별도의 `ToolCallingLocalModelTest`에서 확인합니다.

## 확인 질문

상품 가격과 배송비를 모델이 추측하게 하지 않고 Java 서비스에서 조회·계산하게 하는 이유는 무엇인가요?

<details>
<summary>해설</summary>

모델의 답변만으로 실제 상품 가격과 배송 정책이 맞다고 보장할 수 없기 때문입니다. Java 서비스가 카탈로그와 배송 규칙으로 값을 확인하고, 이후 모델은 도구로 받은 결과를 답변에 사용하도록 역할을 나눕니다.

</details>

[이전](../05-tool-calling.md) · [다음](02-tools.md) · [단원 목차](../05-tool-calling.md) · [학습 방법](../../learning-guide.md)
