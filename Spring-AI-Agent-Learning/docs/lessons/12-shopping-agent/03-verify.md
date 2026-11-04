# 12-3. 조회한 사실로 최종 결과 확인

**실행 코드·설정·테스트:** [labs/12-shopping-agent](../../../labs/12-shopping-agent/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

30~45분을 기준으로 한 번에 한 가지 흐름만 읽습니다. 읽기와 실습을 합친 시간이며, 단원 전체 예상 시간에 포함됩니다.

**이번 목표:** 상품 총액과 출처 ID를 실제 도구 기록으로 검증합니다.

**코드 읽기 안내:** 선택한 ID가 있다는 것과 실제로 조회했다는 것은 다릅니다. TurnFacts에서 찾지 못하는 ID는 거절합니다.

## 3.2 모델이 반환한 ID도 검사하기

`Selection`에는 `productIds`, `sourceIds`만 있습니다. 자연어 답변 문장은 받지 않습니다.
아래 `verify()`가 실제 도구 결과와 비교하고, 최종 화면 문장을 Java로 만듭니다.

```java
    private Draft verify(Intent intent, Selection selected, TurnFacts facts) {
        if (selected == null || selected.productIds() == null || selected.sourceIds() == null)
            throw new IllegalArgumentException("선택 형식이 올바르지 않습니다.");
        List<Offer> offers = new ArrayList<>();
        for (String id : selected.productIds().stream().distinct().toList()) {
            Offer offer = facts.offers.get(id);
            if (!intent.task().equals("SHOPPING") || offer == null || offer.total() > intent.totalBudget()
                    || (intent.category() != null && !offer.product().category().equals(intent.category())))
                return invalid(intent);
            offers.add(offer);
        }
        List<Source> sources = new ArrayList<>();
        for (String id : selected.sourceIds().stream().distinct().toList()) {
            Source source = facts.sources.get(id);
            if (source == null) return invalid(intent);
            sources.add(source);
        }
        if (intent.task().equals("SHOPPING") && offers.isEmpty()) {
            boolean noAffordableProduct = facts.productSearched && facts.lastProducts.stream().allMatch(product -> {
                Offer checked = facts.offers.get(product.id());
                return checked != null && checked.total() > intent.totalBudget();
            });
            String status = noAffordableProduct ? "NO_RESULTS" : "NO_VERIFIED_OFFER";
            return new Draft(status, "현재 조건에서 확인된 추천 상품이 없습니다.", intent, List.of(), List.of());
        }
        if (intent.task().equals("POLICY") && sources.isEmpty()) {
            String status = facts.policySearched && facts.sources.isEmpty() ? "NO_CONTEXT" : "NO_VERIFIED_SOURCE";
            return new Draft(status, "확인할 수 있는 정책 근거가 없습니다.", intent, List.of(), List.of());
        }
        // 자유 생성 문장 대신 검증한 실제 총액과 정책 원문으로 표시한다.
        String answer = offers.stream().map(offer -> offer.product().name() + " 총 " + offer.total() + "원")
                .collect(java.util.stream.Collectors.joining(" / "));
        if (!sources.isEmpty()) answer += "\n정책 근거: " + sources.stream().map(Source::text).collect(java.util.stream.Collectors.joining(" / "));
        return new Draft("VERIFIED", answer.strip(), intent, List.copyOf(offers), List.copyOf(sources));
    }
```

배송비를 조회하지 않은 상품, 예산 초과 상품, 카테고리 불일치, 검색하지 않은 정책 ID는 거절합니다.
검색된 모든 상품의 총액이 예산을 넘는 것도 실제 조회 결과로 확인해야 `NO_RESULTS`가 됩니다.
모델이 그냥 빈 목록을 골랐다는 이유만으로 “상품이 없다”고 확정하지 않습니다.

`VERIFIED`는 이 예제의 금액·ID·조건 검사를 통과했다는 뜻입니다.
정책 문서 자체의 진실성, 질문과 근거의 의미적 적합성, 최선의 상품 선택까지 보장하지 않습니다.
또한 **사용자가 요청한 모든 항목에 답했는지**도 검사하지 않습니다. 예를 들어 “상품 추천과 반품 기간”을 함께 물어도,
모델이 유효한 상품만 선택하고 `sourceIds`를 빈 목록으로 반환하면 현재 검증은 `VERIFIED`를 반환할 수 있습니다.
따라서 이 상태를 “요청 전체 처리 완료”로 해석하면 안 됩니다. 12-4의 표는 기대 시나리오이며 모든 실제 실행의 보장은 아닙니다.
10장의 자유 생성 답변과 달리 **실제 금액과 정책 원문을 표시**하는 선택을 했습니다.


<details><summary>선택 과제: 검증 범위를 확장한다면</summary>

복합 요청의 누락까지 막아야 하는 서비스라면 필요한 결과 항목을 따로 추출하고, 정책 답변이 필요한데 출처가 없는 경우도 검사해야 합니다. 이번 예제의 검증 범위에는 포함하지 않습니다.
자연스러운 요약을 추가한다면 11장 방식으로 사실 일치도 평가를 먼저 확장해야 합니다.

</details>

## 여기서 멈춰 확인하기

실제 조회하지 않은 ID를 거절하는 코드와, 요청 일부의 누락은 검사하지 않는 한계를 설명해 보세요.

[이전](02-intent.md) · [다음](04-conversation.md) · [단원 목차](../12-shopping-agent.md) · [학습 방법](../../learning-guide.md)
