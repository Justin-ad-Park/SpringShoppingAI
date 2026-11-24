# 14-1. Choice로 쇼핑 의도 분류하기

**목표:** 고객 메시지를 정해진 의도로 분류하고, 확실하지 않은 결과는 확인 질문 대상으로 구분합니다.

**실행 프로젝트:** [14-00-jev-decisions](../../../labs/14-00-jev-decisions/README.md).<br>
처음 실행한다면 README의 키·설정·실행 안내를 먼저 읽습니다.

## 무엇을 선택할까?

“3만원 이하의 텀블러를 추천해 주세요”는 상품 검색, “주문한 물건이 어디 있나요?”는 주문·배송 문의입니다.<br>
Choice는 애플리케이션이 제공한 선택지 중 하나를 고릅니다.<br>
조건이 맞지 않는 입력도 들어오므로 `OTHER`를 포함합니다.

| 선택지 | 의미 |
|---|---|
| PRODUCT_SEARCH | 상품 검색·추천·특징·가격 문의 |
| ORDER_DELIVERY | 이미 주문한 상품의 주문 상태·배송 문의 |
| RETURN_EXCHANGE | 반품·교환 문의 |
| OTHER | 범위 밖 또는 하나로 정하기 어려운 복수 의도 |

이 단순 분류표는 이번 실습의 업무 기준입니다.<br>
복수 의도의 세밀한 분리나 계층형 분류는 [13-5](../13-shopping-agent/05-intent-classification.md)에서 비교합니다.

## 코드 읽기

[Lesson01IntentClassificationService](../../../labs/14-00-jev-decisions/src/main/java/com/example/ailearning/l14jev/lesson01_choice/Lesson01IntentClassificationService.java)의 `INTENT_QUESTION`이 각 선택지의 설명을 정의합니다.<br>
다음은 `classifyIntent()`의 실제 발췌입니다.

```java
    public IntentClassificationResult classifyIntent(String message) {
        SystemOneRequest intentRequest = SystemOneRequest.builder()
                .state(message)
                .question("intent", INTENT_QUESTION)
                .build();
        SystemOneResponse intentResponse = typeSafeClient.systemOne(intentRequest);
        ChoiceAnswer intentAnswer = intentResponse.choice("intent");
        ShoppingIntent intent = ShoppingIntent.valueOf(intentAnswer.value());
        boolean clarificationRequired = SupportRoutingPolicy.requiresIntentClarification(intent, intentAnswer.confidence());
        return new IntentClassificationResult(intent, intentAnswer.confidence(), intentAnswer.probabilities(), clarificationRequired);
    }
```

`intent`는 요청과 응답을 연결할 질문 ID입니다.<br>
`.build()`는 요청 객체를 만들고, `systemOne()`을 호출할 때 HTTP 요청이 전송됩니다.<br>
Starter가 만든 TypeSafeClient는 서비스 생성자로 주입됩니다.

`SystemOneResponse`는 Jev API 전체 응답이고, `ChoiceAnswer`는 그 안에서 읽은 의도 선택 결과입니다.<br>
서비스는 선택 의도·confidence·항목별 확률·확인 질문 필요 여부를 담은 `IntentClassificationResult`를 반환합니다.

응답의 선택 항목은 `ShoppingIntent` Enum으로 변환합니다.<br>
[SupportRoutingPolicy](../../../labs/14-00-jev-decisions/src/main/java/com/example/ailearning/l14jev/domain/SupportRoutingPolicy.java)는 confidence가 **0.7 미만**이거나 결과가 `OTHER`이면 `clarificationRequired=true`로 정합니다.<br>
모델의 선택 결과와 서비스가 그 선택을 사용해도 되는지는 구분합니다.

## 요청과 결과 관찰

앱 실행 뒤 저장소 루트 또는 다른 터미널에서 호출합니다.

```bash
curl -X POST http://localhost:8114/api/jev/intent -H 'Content-Type: text/plain; charset=UTF-8' --data '3만원 이하의 텀블러를 추천해 주세요.'
```

다음은 실제 측정값이 아닌 **응답 형태 예시**입니다.

```json
{"predictedIntent":"PRODUCT_SEARCH","confidence":0.85,"probabilities":{"PRODUCT_SEARCH":0.9,"ORDER_DELIVERY":0.04,"RETURN_EXCHANGE":0.03,"OTHER":0.03},"clarificationRequired":false}
```

`probabilities`는 항목별 확률, `confidence`는 분포 집중도에 따른 신뢰도입니다.<br>
높은 confidence도 정확성을 보증하지 않으며 고객 메시지의 내용만 분류할 뿐 주문 소유권을 확인하지 않습니다.

## 검증

[Lesson01JevChoiceLocalModelTest](../../../labs/14-00-jev-decisions/src/test/java/com/example/ailearning/l14jev/lesson01_choice/Lesson01JevChoiceLocalModelTest.java)는 `state`·선택지·모델명·더미 인증 헤더가 실제 HTTP 요청에 담기는지와 낮은 confidence에서 보류하는지 검사합니다.<br>
실제 Jev의 분류 품질은 확인하지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:14-00-jev-decisions:test --tests '*Lesson01JevChoiceLocalModelTest.Choice*'
```

실제 Jev의 판단은 [Lesson01JevChoiceLiveTest](../../../labs/14-00-jev-decisions/src/test/java/com/example/ailearning/l14jev/lesson01_choice/Lesson01JevChoiceLiveTest.java)에서 대조 입력 두 개로 확인할 수 있습니다.<br>
[Live 기능별 선택 실행](../../../labs/14-00-jev-decisions/README.md#live-기능별-선택-실행) 명령은 실제 API를 호출하며 사용량이 발생합니다.

## 확인 질문

1. 낮은 confidence에서도 Choice가 항목 하나를 반환했다면 바로 업무를 실행해야 할까요?
2. `OTHER`를 선택지에 넣는 이유는 무엇인가요?

<details>
<summary>해설</summary>

1. 아닙니다.<br>
   이 예제는 confidence가 0.7 미만이면 확인 질문 대상으로 보류합니다.<br>
   실제 적용 기준은 대표 고객 요청으로 검증해야 합니다.
2. 분류 범위 밖의 문장이나 복수 의도를 기존 범주에 억지로 넣지 않도록 선택 가능성을 제공합니다.<br>
   다만 OTHER가 있다고 모호한 입력을 항상 정확히 식별하는 것은 아닙니다.

</details>

근거: [TypeSafe Choice](https://docs.typesafe.ai/primitives/choice).

[14장 목차](../14-jev.md) · [다음: Score](02-score-priority.md)
