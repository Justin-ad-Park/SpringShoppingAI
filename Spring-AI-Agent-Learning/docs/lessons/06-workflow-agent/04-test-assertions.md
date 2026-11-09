# 06장 보충. 테스트 검증 코드 읽기

[06장](../06-workflow-agent.md)의 세 실행 방식을 읽은 뒤, 테스트 검증문이 낯설 때 보는 **선택 보충**입니다.
설명할 코드는 [WorkflowAgentOpenAiLiveTest](../../../labs/06-00-workflow-agent/src/test/java/com/example/ailearning/l06decisions/WorkflowAgentOpenAiLiveTest.java)에 있습니다.

**이번 목표:** 추천 결과의 값 검사와 실제 메서드 호출 기록 검사를 구분해 테스트를 읽습니다.

## 1. 결과를 검사하는 AssertJ와 호출 기록을 검사하는 Mockito

아래 기능들은 AI 기능이 아니라 테스트 라이브러리의 기능입니다.

| 라이브러리 | 메서드 | 확인하는 대상 |
|---|---|---|
| AssertJ | `assertThat`, `extracting`, `containsExactly`, `allSatisfy` | 서비스가 반환한 값과 목록 |
| Mockito | `verify`, `atLeastOnce`, `any`, `anyString` | Mock·Spy에 남은 메서드 호출 기록 |

`assertThat`은 `org.assertj.core.api.Assertions`에서, Mockito 메서드는 `org.mockito.Mockito`에서 정적으로 가져옵니다. 테스트의 `import static ...`이 이 연결을 나타냅니다.
Spy와 `InOrder`의 준비·기본 동작은 [05-4 Spy로 호출 순서와 인자 검증하기](../05-tool-calling/04-spy-and-inorder.md)에서 확인합니다.

## 2. extracting과 containsExactly: 추천 상품의 ID 검사하기

다음은 Workflow 테스트의 발췌입니다. `result`는 앞에서 `service.run(...)`으로 받은 결과이며, `result.offers()`는 추천 후보 목록입니다.

```java
assertThat(result.offers())
        .extracting(offer -> offer.product().id())
        .containsExactly("P01");
```

1. `assertThat(result.offers())`: 추천 후보 목록을 검사 대상으로 지정합니다.
2. `extracting(offer -> offer.product().id())`: 각 후보에서 상품 ID를 꺼내 그 값들을 검증합니다. 원래 추천 목록을 변경하거나 P01만 골라내는 필터가 아닙니다.
3. `containsExactly("P01")`: 꺼낸 ID가 **P01 하나뿐인지** 확인합니다. 기대한 항목의 값·개수·순서가 모두 일치해야 합니다.

`offer -> ...`는 목록의 각 후보를 받아 오른쪽 값을 구하는 Java 람다입니다.
예를 들어 `extracting` 이후 검사할 ID들이 다음과 같다면 결과는 아래와 같습니다. 실제 실행 로그가 아닌 설명용 예시입니다.

| 검사할 ID 목록 | 결과 | 이유 |
|---|---|---|
| `[P01]` | 통과 | 기대한 ID 하나만 존재 |
| `[]` | 실패 | P01이 없음 |
| `[P01, P02]` | 실패 | 추가 상품이 있음 |
| `[P01, P01]` | 실패 | 중복으로 개수가 다름 |

이 예제는 기대값이 하나여서 순서 차이가 드러나지 않습니다. `containsExactly("P01", "P02")`라면 `[P02, P01]`도 실패합니다.
Workflow에서 P01을 기대하는 이유는 현재 상품 데이터와 배송비 규칙입니다. P01은 총 15000원, P02는 총 31000원이므로 총예산 30000원에서는 P01만 남습니다.

## 3. allSatisfy: 모든 추천 후보에 같은 검사 적용하기

다음은 혼합 방식 테스트의 발췌입니다. `productCatalog`는 클래스에 선언한 `@MockitoSpyBean`입니다.

```java
assertThat(result.offers()).isNotEmpty();
assertThat(result.offers())
        .allSatisfy(offer -> {
            verify(productCatalog, atLeastOnce()).calculatePriceDetails(offer.product().id());
            assertThat(offer.product().category()).isEqualTo("음료");
            assertThat(offer.total()).isLessThanOrEqualTo(30000);
        });
```

`allSatisfy(offer -> { ... })`는 목록의 각 후보에 중괄호 안의 검사를 적용합니다. 모든 후보가 다음 조건을 만족해야 통과합니다.

- 해당 상품 ID로 가격 계산 메서드를 호출한 기록이 있음.
- 카테고리가 음료임.
- 배송비 포함 총액이 30000원 이하임.

**`allSatisfy`만으로는 후보가 존재하는지 확인하지 못합니다.** 빈 목록이면 검사할 항목이 없어 통과하므로, 앞의 `isNotEmpty()`로 추천 후보가 하나 이상인지 별도로 확인합니다.

## 4. verify와 atLeastOnce: 실제 조회가 있었는지 확인하기

앞 코드의 다음 한 줄은 값을 계산하는 코드가 아니라 Mockito의 호출 기록 검사입니다.

```java
verify(productCatalog, atLeastOnce()).calculatePriceDetails(offer.product().id());
```

| 표현 | 읽는 방법 |
|---|---|
| `productCatalog` | 호출 기록을 읽을 Spy |
| `atLeastOnce()` | 같은 인자의 호출이 한 번 이상 있어야 함. 반복 호출도 허용 |
| `.calculatePriceDetails(offer.product().id())` | 해당 후보의 상품 ID로 이 메서드를 호출했는지 검사 |

예를 들어 후보의 ID가 P01이면, 이전 `service.run(...)` 실행 중 `calculatePriceDetails("P01")`을 한 번 이상 호출했는지 확인합니다. 이 검증문이 가격을 다시 계산하거나 모델을 다시 호출하지는 않습니다.
일반 `verify`는 호출 순서를 검사하지 않습니다. 순서가 고정된 Workflow는 `InOrder`로 검사하고, 모델이 순서를 선택하는 Agent·혼합 방식은 조회 기록과 결과 조건을 확인합니다.

Agent 테스트의 `verify(productCatalog, atLeastOnce()).search(any(), any())`는 인자 값을 특정하지 않고 검색 호출이 한 번 이상 있었는지 확인합니다. 여기의 `any()`는 **검증에서 사용할 인자 조건**이며, 실제 검색 요청에 전달하는 값이 아닙니다.
Agent 테스트는 `verify(productCatalog, atLeastOnce()).calculatePriceDetails(anyString())`도 검사하므로, 검색과 가격 조회가 모두 한 번 이상 실행되어야 통과합니다. `anyString()`은 null이 아닌 어떤 문자열 인자든 허용한다는 검증 조건입니다.
혼합 방식의 `verify(productCatalog, atLeastOnce()).search("음료", 30000)`는 Java가 고정한 인자로 실제 검색했는지까지 확인합니다.

카탈로그의 호출 기록은 도구 내부에서 실행한 조회·계산 기록입니다. 도구와 카탈로그의 같은 메서드명·인자 처리 관계는 [실습 README](../../../labs/06-00-workflow-agent/README.md#실제-모델-연결)를 참고하세요.
호출이 있었다는 사실만으로 계산 결과나 모델 답변 전체가 정확하다고 보장하지는 않습니다. 값 검사와 호출 기록 검사는 서로 다른 사실을 확인합니다.

## 확인 질문

1. `extracting`이 P01만 남기도록 추천 목록을 변경하나요?
2. `allSatisfy` 앞에서 `isNotEmpty`를 검사하는 이유는 무엇인가요?
3. `verify(..., atLeastOnce())`는 메서드를 다시 호출하거나 실행 순서를 정하나요?

<details>
<summary>해설</summary>

1. 아닙니다. `extracting`은 각 항목에서 검증할 값을 꺼내며 원래 목록을 변경하거나 P01만 남기지 않습니다.
2. `allSatisfy`는 빈 목록도 통과하므로 후보가 하나 이상 있는지는 `isNotEmpty()`로 따로 확인해야 합니다.
3. 아닙니다. `verify`는 이미 남은 기록을 검사하고 메서드를 재실행하지 않습니다. `atLeastOnce`는 횟수 조건이며 순서 조건이 아닙니다.

</details>

[06장 목차로 돌아가기](../06-workflow-agent.md) · [05-4 Spy·InOrder 설명](../05-tool-calling/04-spy-and-inorder.md) · [학습 방법](../../learning-guide.md)
