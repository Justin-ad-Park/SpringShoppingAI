# 13-3. 조회한 사실로 최종 결과 확인

**실행 코드·설정·테스트:** [labs/13-00-shopping-agent](../../../labs/13-00-shopping-agent/README.md).<br>
저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 상품 총액과 출처 ID를 실제 도구 기록으로 검증합니다.

**코드 읽기 안내:** 선택한 ID가 있다는 것과 실제로 조회했다는 것은 다릅니다.<br>
`CounselToolResults`에서 찾지 못하는 ID는 거절합니다.

## 3.2 모델이 반환한 ID도 검사하기

`ProductPriceDetail`은 상품 한 건의 금액 정보이며, 여러 상품은 `List<ProductPriceDetail>`로 표현합니다.<br>
`CandidateEvidenceSelection`에는 `productIds`, `sourceIds`만 있습니다.<br>
자연어 답변 문장은 받지 않습니다.<br>
`Lesson03CounselResultVerifier.verifySelection()`이 실제 도구 결과와 비교합니다.<br>
검증 후 `createVerifiedAnswer()`가 실제 금액과 정책 원문으로 표시 문장을 만듭니다.

```java
    public CounselResult verifySelection(Intent requestIntent,
            CandidateEvidenceSelection candidateSelection, CounselToolResults toolResults) {
        if (hasInvalidSelection(candidateSelection)) {
            return verificationFailed(requestIntent);
        }

        List<ProductPriceDetail> verifiedOffers = new ArrayList<>();
        for (String productId : candidateSelection.productIds().stream().distinct().toList()) {
            ProductPriceDetail priceDetail = toolResults.priceDetailFor(productId);
            if (!isValidProductSelection(requestIntent.task().equals("SHOPPING"), requestIntent, priceDetail)) {
                return verificationFailed(requestIntent);
            }
            verifiedOffers.add(priceDetail);
        }

        // 정책 ID도 이번 요청의 검색 결과에서 찾아야 답변 근거로 사용할 수 있다.
        List<PolicySearchResult> verifiedSources = new ArrayList<>();
        for (String sourceId : candidateSelection.sourceIds().stream().distinct().toList()) {
            PolicySearchResult policyResult = toolResults.policyResultFor(sourceId);
            if (policyResult == null) {
                return verificationFailed(requestIntent);
            }
            verifiedSources.add(policyResult);
        }

        if (requestIntent.task().equals("SHOPPING") && verifiedOffers.isEmpty()) {
            String resultStatus = hasNoAffordableProduct(requestIntent, toolResults)
                    ? "NO_RESULTS" : "NO_VERIFIED_OFFER";
            return new CounselResult(resultStatus, "현재 조건에서 확인된 추천 상품이 없습니다.",
                    requestIntent, List.of(), List.of());
        }
        if (requestIntent.task().equals("POLICY") && verifiedSources.isEmpty()) {
            String resultStatus = toolResults.policySearchPerformed() && !toolResults.hasPolicySearchResults()
                    ? "NO_CONTEXT" : "NO_VERIFIED_SOURCE";
            return new CounselResult(resultStatus, "확인할 수 있는 정책 근거가 없습니다.",
                    requestIntent, List.of(), List.of());
        }
        return new CounselResult("VERIFIED", createVerifiedAnswer(verifiedOffers, verifiedSources),
                requestIntent, verifiedOffers, verifiedSources);
    }
```

배송비를 조회하지 않은 상품, 예산 초과 상품, 카테고리 불일치, 검색하지 않은 정책 ID는 거절합니다.<br>
검색된 모든 상품의 총액이 예산을 넘는 것도 실제 조회 결과로 확인해야 `NO_RESULTS`가 됩니다.<br>
모델이 그냥 빈 목록을 골랐다는 이유만으로 “상품이 없다”고 확정하지 않습니다.

`VERIFIED`는 이 예제의 금액·ID·조건 검사를 통과했다는 뜻입니다.<br>
정책 문서 자체의 진실성, 질문과 근거의 의미적 적합성, 최선의 상품 선택까지 보장하지 않습니다.<br>
또한 **사용자가 요청한 모든 항목에 답했는지**도 검사하지 않습니다.<br>
예를 들어 “상품 추천과 반품 기간”을 함께 물어도,
모델이 유효한 상품만 선택하고 `sourceIds`를 빈 목록으로 반환하면 현재 검증은 `VERIFIED`를 반환할 수 있습니다.<br>
따라서 이 상태를 “요청 전체 처리 완료”로 해석하면 안 됩니다.<br>
13-4의 표는 기대 시나리오이며 모든 실제 실행의 보장은 아닙니다.<br>
10단원의 자유 생성 답변과 달리 **실제 금액과 정책 원문을 표시**하는 선택을 했습니다.

### 10단원의 답변 생성과 어떻게 다를까?

| 구분 | 10단원 직접 RAG | 13단원 기본 상담 |
|---|---|---|
| 모델이 반환하는 것 | 고객에게 보여 줄 답변 문장과 출처 ID | 선택한 상품·정책의 ID |
| Java가 확인하는 것 | 인용 ID가 이번 검색 결과에 있는지 | 상품의 조회 사실·금액·조건과 정책 조회 사실 |
| 최종 문장 작성 | 모델이 검색 원문에서 필요한 내용을 골라 작성 | `createVerifiedAnswer()`가 실제 금액과 선택된 정책 원문을 연결 |

13단원은 상품 금액과 선택한 근거를 대조하는 흐름에 집중해, 최종 문장을 Java로 구성합니다.<br>
10단원의 생성 단계를 통합 샘플에서 그대로 재사용한 것은 아닙니다.<br>
모델의 자연스러운 정책 요약이 필요하면 10단원의 생성 방식을 별도 단계로 연결하고, 원문의 조건·예외가 유지되는지 평가해야 합니다.


<details><summary>선택 과제: 검증 범위를 확장한다면</summary>

복합 요청의 누락까지 막아야 하는 서비스라면 필요한 결과 항목을 따로 추출하고, 정책 답변이 필요한데 출처가 없는 경우도 검사해야 합니다.<br>
이번 예제의 검증 범위에는 포함하지 않습니다.<br>
이 구분을 실행해 보는 선택 실습은 [13-9 요청한 결과의 누락 확인](09-result-completeness.md)에 있습니다.<br>
자연스러운 요약을 추가한다면 12단원 방식으로 사실 일치도 평가를 먼저 확장해야 합니다.

</details>

## 확인 질문

실제 조회하지 않은 ID를 거절하는 코드와, 요청 일부의 누락은 검사하지 않는 한계를 설명해 보세요.

<details>
<summary>해설</summary>

`Lesson03CounselResultVerifier.verifySelection()`이 상품 ID는 `CounselToolResults.priceDetailFor()`에서, 정책 ID는 `policyResultFor()`에서 찾아 조회 사실이 없으면 거절합니다.<br>
다만 상품과 정책을 함께 요청했는지까지 검사하지 않아, 유효한 상품만 선택하고 정책 출처를 누락해도 `VERIFIED`가 될 수 있습니다.

</details>

[이전](02-intent.md) · [다음](04-conversation.md) · [단원 목차](../13-shopping-agent.md) · [학습 방법](../../learning-guide.md)
