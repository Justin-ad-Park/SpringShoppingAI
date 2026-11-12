# 06장 보충. 테스트 검증 코드 읽기

[06장](../06-workflow-agent.md)의 세 실행 방식을 읽은 뒤, 테스트 검증문이 낯설 때 보는 **선택 보충**입니다.
[Live 테스트](../../../labs/06-00-workflow-agent/src/test/java/com/example/ailearning/l06decisions/WorkflowAgentOpenAiLiveTest.java), [로컬 테스트](../../../labs/06-00-workflow-agent/src/test/java/com/example/ailearning/l06decisions/WorkflowAgentLocalModelTest.java), [도구 단위 테스트](../../../labs/06-00-workflow-agent/src/test/java/com/example/ailearning/l06decisions/BudgetProductToolsUnitTest.java)를 함께 봅니다.

**이번 목표:** 반환값 검사, 실제 메서드 호출 기록 검사, 모델에 전달된 메시지 검사를 구분합니다.

## 1. 검증 메서드 지도: 표 순서대로 읽기

AssertJ는 **반환된 값**, Mockito는 **이미 실행된 호출 기록**을 검사합니다. 이 페이지에서 연결한 06장 테스트의 검증 메서드를 아래 순서대로 설명합니다. `search`, `calculatePriceDetail` 등은 검사 대상인 업무 메서드입니다.

| 라이브러리 | 메서드·표현 | 기능 | 설명 절 |
|---|---|---|---|
| AssertJ | `assertThat(value)` | 검사할 값을 지정하고 검증 메서드를 연결 | 1 |
| AssertJ | `isEqualTo(expected)` | 실제 값이 기대 값과 같은지 검사 | 1 |
| AssertJ | `isNotBlank()` | 문자열이 null·빈 문자열·공백만 있는 문자열이 아닌지 검사 | 1 |
| AssertJ | `extracting(...)` | 목록의 각 항목에서 검사할 값을 꺼냄 | 2 |
| AssertJ | `containsExactly(...)` | 값·개수·순서가 기대 목록과 같은지 검사. 중복 개수도 포함 | 2 |
| AssertJ | `hasSize(count)` | 목록 등의 항목 수가 기대한 개수인지 검사 | 2 |
| AssertJ | `isEmpty()` | 목록 등에 항목이 없는지 검사 | 2 |
| AssertJ | `isZero()` | 숫자가 0인지 검사 | 2 |
| AssertJ | `contains(...)` | 문자열에 지정한 부분 문자열이 모두 포함되는지 검사 | 3 |
| AssertJ | `doesNotContain(...)` | 문자열에 지정한 부분 문자열이 하나도 없는지 검사 | 3 |
| AssertJ | `isTrue()` | 계산한 논리값이 true인지 검사 | 3 |
| Mockito | `verify(object)` | 해당 객체의 메서드 호출 기록 검사. 횟수를 생략하면 정확히 1회 | 4 |
| Mockito | `atLeastOnce()` | 호출 횟수를 1회 이상으로 지정 | 4 |
| Mockito | `never()` | 지정한 메서드·인자로 호출된 횟수를 0회로 지정 | 4 |
| Mockito | `any()` | 검증할 인자로 어떤 값이든 허용. null도 포함 | 4 |
| Mockito | `anyString()` | 검증할 인자로 null이 아닌 문자열을 허용 | 4 |
| Mockito | `eq(value)` | 검증할 인자가 지정한 값과 같은 경우만 허용 | 4 |
| Mockito | `verifyNoInteractions(...)` | 지정한 객체들에 메서드 호출 자체가 없었는지 검사 | 5 |
| Mockito | `inOrder(...)` | 지정한 객체들의 호출 기록을 순서대로 검사할 객체 생성 | 6 |
| Mockito | `order.verify(object)` | `inOrder`로 만든 객체에서 다음 순서의 호출 검사 | 6 |
| Mockito | `verifyNoMoreInteractions(...)` | 검증하지 않은 호출이 더 남아 있지 않은지 검사 | 6 |

`import static org.assertj.core.api.Assertions.assertThat`과 `import static org.mockito.Mockito.*`가 이 메서드를 가져옵니다.
Spy의 준비는 [05-4 Spy와 InOrder](../05-tool-calling/04-spy-and-inorder.md)에서 설명합니다.
`@MockitoSpyBean ProductCatalog`는 실제 검색·계산을 실행하면서 기록도 남깁니다.

### assertThat → isEqualTo → isNotBlank: 기본 값 검사

다음은 Live 테스트의 공통 검증 메서드에서 사용하는 구문입니다. `result`는 이미 실행한 상담 결과입니다.

```java
assertThat(result.status()).isEqualTo("COMPLETED");
assertThat(result.answer()).isNotBlank();
```

`assertThat`은 검사할 값을 받습니다. 첫 줄의 `isEqualTo`는 상태가 `COMPLETED`와 같은지, 둘째 줄의 `isNotBlank`는 답변에 공백 이외의 문자가 있는지 검사합니다. `isNotBlank`가 통과해도 답변 내용이 사실이라는 뜻은 아닙니다.

## 2. extracting과 containsExactly: 상품 ID 목록 검사하기

### extracting: 목록에서 검사할 값 꺼내기

다음은 `BudgetProductToolsUnitTest`의 발췌입니다. `products`는 도구가 반환한 조회 결과입니다.

```java
assertThat(products)
        .extracting(detail -> detail.product().id())
        .containsExactly("P01");
assertThat(products.getFirst().total()).isEqualTo(15000);
```

`extracting(detail -> detail.product().id())`는 각 항목에서 상품 ID를 꺼내 이후 검증의 대상으로 삼습니다. 원래 목록을 변경하는 필터가 아닙니다.
3만 원 예산에서 허브티는 총 15000원이므로 남고, 커피는 배송비를 더한 31000원으로 제외됩니다. 첫 검증은 남은 상품의 ID를, 두 번째 검증은 첫 상품의 총액을 확인합니다.

### containsExactly: 값·개수·순서 검사하기

`containsExactly("P01")`는 ID 목록이 P01 하나로만 구성됐는지 확인합니다. 여러 값을 기대하면 순서도 검사합니다.

두 상품의 ID가 `[P01, P02]`인 경우에 통과하는 예시도 보겠습니다. 다음은 실습의 [Product](../../../labs/06-00-workflow-agent/src/main/java/com/example/ailearning/l06decisions/domain/Product.java) 두 개를 직접 준비하는 **테스트 메서드 안의 설명용 예시**입니다. `java.util.List`와 해당 `Product`를 import한 상태에서 사용합니다.

```java
List<Product> foundProducts = List.of(
        new Product("P01", "허브티", "음료", 12000),
        new Product("P02", "커피", "음료", 28000));

assertThat(foundProducts)
        .extracting(product -> product.id()) // 검사할 ID 목록: [P01, P02]
        .containsExactly("P01", "P02");      // 값·개수·순서가 모두 같으므로 통과
```

기대 목록이 `[P01, P02]`일 때 결과는 다음과 같습니다.

| 실제 ID 목록 | 결과 | 이유 |
|---|---|---|
| `[P01, P02]` | 통과 | 값·개수·순서가 같음 |
| `[P01]` | 실패 | P02 누락 |
| `[P01, P02, P03]` | 실패 | 추가 항목 존재 |
| `[P02, P01]` | 실패 | 순서가 다름 |
| `[P01, P01, P02]` | 실패 | 중복 항목으로 개수가 다름 |
| `[]` | 실패 | 기대한 두 항목이 없음 |

실제 목록이 `[P01, P02]`여도 기대 값을 `"P01"` 하나만 적으면 추가 항목 P02 때문에 실패합니다.

### hasSize → isEmpty → isZero: 개수·빈 목록·금액 검사

다음 세 구문은 각각 단위 테스트와 로컬 테스트에서 사용합니다. 각 변수는 해당 테스트에서 실행한 결과입니다.

```java
assertThat(products).hasSize(count);
assertThat(MODEL.chat().getRequests()).isEmpty();
assertThat(products.getFirst().shippingFee()).isZero();
```

- `hasSize(count)`는 개수만 검사합니다. 배송비 포함 총액이 15000원인 상품은 예산 15000원에서 1개, 14999원에서 0개가 되는 경계를 확인합니다. 상품 ID·순서는 따로 검사해야 합니다.
- `isEmpty()`는 목록이 비었는지 검사합니다. 소문자 실행 방식이 HTTP 400으로 거부되는 테스트에서는 모델 서버에 전달된 요청이 0개인지 확인합니다. null은 빈 목록으로 인정하지 않습니다.
- `isZero()`는 숫자 0을 검사합니다. 생활 카테고리의 텀블러를 조회한 단위 테스트에서는 배송비가 0원인지 확인합니다.

단위 테스트는 카테고리를 `생활`로 지정하면 P03만 반환하는 경우와, 카테고리를 생략하면 P01·P02·P03을 반환하는 경우도 `extracting`·`containsExactly`로 검사합니다.

## 3. contains와 doesNotContain, isTrue: 전송 메시지와 조건 검사

### contains → doesNotContain: 문자열에 필요한 내용이 있는지 검사

다음은 Workflow 로컬 테스트의 발췌입니다.

```java
assertThat(MODEL.chat().getRequests().getLast().path("messages").toString())
        .contains("P01", "15000")
        .doesNotContain("P02", "31000");
```

`contains`는 문자열에 P01과 15000이 모두 있는지 검사합니다. 다른 내용이 함께 있어도 되고 두 문자열의 순서는 제한하지 않습니다.
`doesNotContain`은 P02와 31000이 어느 것도 없는지 검사합니다. 예산을 초과한 후보를 모델의 설명 요청에 넣지 않았는지 확인하는 용도입니다.
Agent 테스트에서는 답변에 상품 이름이 있는지, 도구 결과 메시지에 실제 조회 금액이 있는지도 `contains`로 검사합니다.

여기서 `getRequests()`는 모델 서버가 받은 요청 목록, `getLast()`는 마지막 요청, `path("messages")`는 JSON의 messages 필드를 가져옵니다. `toString()`은 그 JSON을 검사할 문자열로 바꿉니다. 이 호출들은 검사할 값을 준비하고, AssertJ가 그 값을 검사합니다.

### isTrue: 계산한 조건이 참인지 검사

다음은 Workflow가 모델에 도구를 제공하지 않는지 확인하는 구문입니다. `request`는 모델 서버가 받은 요청 JSON입니다.

```java
assertThat(request.path("tools").isMissingNode() || request.path("tools").isEmpty()).isTrue();
```

JSON의 tools 필드가 없거나 빈 배열이면 `||`(또는) 조건이 true가 되어 통과합니다. 도구가 하나라도 들어 있으면 실패합니다.
Hybrid에서는 `tool.at("/function/parameters/properties").isEmpty()`가 true인지 검사해 모델이 도구 인자로 검색 조건을 다시 지정할 수 없는지 확인합니다. `at`은 `/`로 구분한 JSON 경로를 읽습니다.
여기의 `JsonNode.isEmpty()`는 논리값을 반환하고, 2절의 `assertThat(list).isEmpty()`는 목록을 검증하는 AssertJ 메서드라는 차이가 있습니다.

## 4. verify와 호출 횟수·인자 조건: 이미 실행된 기록 검사

### verify: 지정한 메서드가 정확히 한 번 호출됐는지 검사

```java
verify(productCatalog).search("음료", 30000);
```

이미 실행된 기록에서 같은 인자로 검색한 호출이 정확히 1회 있는지 확인합니다. 검색을 다시 실행하지 않습니다. 배송비 조회 같은 다른 메서드의 추가 호출까지 금지하는 검사는 아닙니다.

### atLeastOnce: 1회 이상 허용

```java
verify(productCatalog, atLeastOnce()).search("음료", 30000);
```

동일한 조건으로 검색한 기록이 1회 이상이면 통과합니다. Hybrid가 Java에서 확정한 카테고리·예산을 실제 조회에 사용했는지 확인합니다.
실제 모델은 재검색하거나 여러 상품을 비교할 수 있어 Agent·Hybrid의 호출 횟수와 순서를 고정하지 않습니다.

### never: 지정한 호출이 없어야 함

```java
verify(productCatalog, never()).calculatePriceDetail("P01");
```

P01의 금액을 조회한 기록이 0회여야 통과합니다. 이 구문만으로 P02 등 다른 상품의 조회까지 금지하지는 않습니다. 모든 문자열 인자의 조회를 검사할 때는 아래 `anyString()`을 사용합니다.

### any → anyString → eq: 검사할 인자 범위 지정

```java
verify(productCatalog, atLeastOnce()).search(any(), any());
verify(productCatalog, atLeastOnce()).calculatePriceDetail(anyString());
verify(productCatalog, atLeastOnce()).search(eq("음료"), any());
verify(productCatalog, never()).calculatePriceDetail(anyString());
```

- `any()`는 인자가 어떤 값이어도 해당 호출로 셉니다. null도 포함합니다. 첫 줄은 카테고리·가격을 특정하지 않고 검색이 한 번 이상 실행됐는지 확인합니다.
- `anyString()`은 null이 아닌 문자열을 허용합니다. 둘째 줄은 상품 ID를 특정하지 않고 금액 조회가 한 번 이상 실행됐는지 확인합니다.
- `eq("음료")`는 카테고리가 음료인 호출만 셉니다. 셋째 줄은 카테고리를 고정하고 가격 상한에는 어떤 값이든 허용합니다. 같은 호출에서 matcher를 하나라도 쓰면 나머지 인자도 matcher로 표현하므로 `"음료"` 대신 `eq("음료")`를 사용합니다.

이들은 **검증에서 허용할 인자 조건**이며 실제 요청에 전달할 값이 아닙니다.
마지막 줄은 상품 이름만 묻는 요청이나 검색 결과가 없는 경우에 불필요한 금액 조회가 없었는지 검사합니다. `anyString()`은 null을 제외하므로 이 검사도 null 인자 호출까지 확인하는 것은 아닙니다.

## 5. verifyNoInteractions: 조회 자체가 없었는지 검사

```java
verifyNoInteractions(productCatalog);
```

카탈로그에 기록된 메서드 호출이 하나도 없을 때만 통과합니다. 특정 메서드의 특정 인자만 보는 `never()`보다 검사 범위가 넓습니다.
인사, 예산 누락으로 추가 질문, 잘못된 조건 JSON, 잘못된 실행 방식처럼 카탈로그를 호출하기 전에 처리가 끝나야 하는 테스트에서 사용합니다.

## 6. inOrder와 verifyNoMoreInteractions: 고정 순서와 추가 호출 검사

### inOrder → order.verify: Workflow의 고정 순서 검사

```java
var order = inOrder(productCatalog);
order.verify(productCatalog).search("음료", 30000);
order.verify(productCatalog).calculatePriceDetail("P01");
order.verify(productCatalog).calculatePriceDetail("P02");
verifyNoMoreInteractions(productCatalog);
```

`inOrder`는 호출 기록을 순서대로 검사할 객체를 만듭니다. 그 객체의 `order.verify`가 검색 → P01 계산 → P02 계산 순서의 기록을 확인합니다. 메서드를 다시 실행하거나 앞으로의 실행 순서를 지시하지 않습니다.

### verifyNoMoreInteractions: 검증하지 않은 호출이 남았는지 검사

마지막 `verifyNoMoreInteractions`는 위에서 확인한 호출 외에 검증하지 않은 호출이 남아 있으면 실패합니다. 고정 절차를 실행하는 Workflow에 사용합니다.

호출 유무 검사와의 차이는 다음과 같습니다.

| 카탈로그 호출 기록 | `verifyNoInteractions` | `verifyNoMoreInteractions` |
|---|---|---|
| 호출 없음 | 통과 | 통과 |
| 호출이 있고 모두 `verify`로 확인함 | 실패 | 통과 |
| 검증하지 않은 호출이 남아 있음 | 실패 | 실패 |

## 7. 각 테스트가 확인하는 범위

### 도구와 업무 메서드의 연결

| 모델에 공개한 도구 | 내부 카탈로그 호출 |
|---|---|
| Agent의 `searchProducts` | `search` |
| Agent의 `calculateShipping` | `calculatePriceDetail` |
| Hybrid의 `findProductsWithinBudget` | 확정 조건으로 `search`, 후보마다 `calculatePriceDetail`, 총액 필터 |

### 정상·누락·빈 결과·오류 케이스

| 케이스 | 주로 확인하는 값·기록 |
|---|---|
| Workflow 추천 | `COMPLETED`, 고정 조회 순서, 설명 요청에 예산을 통과한 후보만 포함 |
| Agent 추천 | 검색·금액 조회 실행, 답변 및 도구 결과 메시지. 실제 모델의 순서·횟수는 고정하지 않음 |
| Hybrid 추천 | 확정한 검색 조건, 검색 조건 인자가 없는 도구 스키마, 예산을 통과한 상품만 도구 결과에 포함 |
| 예산 누락 | `NEEDS_INPUT`, 모델 요청 1회, 카탈로그 호출 없음 |
| Workflow 검색 결과 없음 | `NO_RESULTS`, 설명용 모델 호출 생략, 금액 조회 없음 |
| Hybrid 검색 결과 없음 | 모델에 빈 도구 결과 `[]` 전달, 모델이 설명한 뒤 `COMPLETED`, 금액 조회 없음 |
| 상품 이름만 질문 | 검색 실행, 금액 조회 없음 |
| 인사 | 빈 문자열이 아닌 답변, 카탈로그 호출 없음 |
| 조건 JSON 변환 실패 | `FAILED`, 카탈로그 호출 없음 |
| 소문자 실행 방식 | HTTP 400, 모델 서버 요청과 카탈로그 호출 없음 |
| 배송비 포함 예산 경계 | 15000원에서 상품 1개, 14999원에서 0개 |
| 카테고리 지정·생략 | 생활이면 P03, 생략하면 P01·P02·P03. 텀블러 배송비 0원 |

로컬 테스트는 고정 모델 응답으로 **Spring AI가 도구를 실행하고 결과를 다시 전달하는 연결**을 재현합니다. 표의 요청 횟수·전송 메시지는 이 고정 시나리오에서 확인한 값입니다.
Live 테스트는 자연스러운 고객 질문으로 **실제 모델의 조건 해석과 도구 선택**을 확인합니다. 결과·호출 기록은 공통 `run` 메서드가, 테스트 이름은 `@BeforeEach`가 출력합니다.

호출 기록은 실행 사실, 단위 테스트는 금액 규칙, 로컬 메시지 검사는 모델에게 전달된 정보를 확인합니다.
`COMPLETED`와 빈 답변 여부·필요한 조회를 검사해도 최종 자연어 문장의 모든 사실과 추천 품질까지 보장하지는 않습니다. Live 테스트는 모델이 필요한 조회를 생략하거나 조건을 잘못 해석하면 실패할 수 있습니다.

## 확인 질문

1. `extracting`은 P01만 남기도록 원래 목록을 변경하나요?
2. `verify(..., atLeastOnce())`가 메서드를 다시 실행하거나 순서를 정하나요?
3. 검색 호출 기록만으로 배송비 포함 예산이 지켜졌다고 판단할 수 있나요?

<details>
<summary>해설</summary>

1. 아닙니다. 각 항목에서 검증할 값을 꺼냅니다. 후보를 제외하는 것은 운영 코드의 `filter`입니다.
2. 아닙니다. 이미 남은 기록을 검사하며, `atLeastOnce`는 횟수 조건입니다. 순서는 `inOrder`로 따로 검사합니다.
3. 아닙니다. 실제 총액과 필터 결과를 확인해야 합니다. 이 단원은 도구 단위 테스트와 모델에 전달된 메시지 검사를 함께 사용합니다.

</details>

[06장 목차로 돌아가기](../06-workflow-agent.md) · [05-4 Spy·InOrder 설명](../05-tool-calling/04-spy-and-inorder.md) · [학습 방법](../../learning-guide.md)
