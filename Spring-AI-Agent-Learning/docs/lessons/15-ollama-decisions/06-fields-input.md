# 15-6. 주문 정보와 고객 메시지를 함께 전달하기

**이번 목표:** 이름 붙은 입력 필드로 문장과 주문 정보를 함께 전달합니다.

“반품하고 싶어요”라는 문장만으로는 해당 주문의 상태를 알 수 없습니다.<br>
이번에는 고객 문장과 주문 상태·배송 후 경과일을 각각 이름 붙인 값으로 전달합니다.<br>
`fields(...)`는 이 값들을 하나의 JSON 객체로 보내는 입력 방식입니다.

## 실행 코드 읽기

[OrderContextDecisionService](../../../labs/15-00-ollama-decisions/src/main/java/com/example/ailearning/l15ollama/OrderContextDecisionService.java)의 전체 코드입니다.

```java
package com.example.ailearning.l15ollama;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionPrompt;
import org.springframework.ai.decision.question.ChoiceQuestion;
import org.springframework.stereotype.Service;

/** 15-6: 고객 메시지와 주문 정보를 이름 붙은 필드로 함께 전달한다. */
@Service
public class OrderContextDecisionService {
    public record OrderInquiry(@NotBlank @Size(max = 2000) String message,
                               @NotBlank @Size(max = 40) String orderStatus,
                               @Min(0) int daysSinceDelivery) {}
    public record OrderContextResult(String nextAction, Map<String, Double> probabilities) {}
    private final DecisionModel decisionModel;

    public OrderContextDecisionService(DecisionModel decisionModel) {
        this.decisionModel = decisionModel;
    }

    public OrderContextResult decideNextAction(OrderInquiry inquiry) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("message", inquiry.message());
        fields.put("orderStatus", inquiry.orderStatus());
        fields.put("daysSinceDelivery", inquiry.daysSinceDelivery());
        var prompt = DecisionPrompt.builder()
                .fields(fields)
                .question("next_action", ChoiceQuestion.builder()
                        .text("고객 메시지와 주문 상태를 함께 보고 어떤 안내가 필요한지 고르세요.")
                        .option("CHECK_DELIVERY", "배송 완료 전 주문의 위치나 도착일을 묻는 문의")
                        .option("EXPLAIN_RETURN", "배송 완료된 주문의 반품 방법을 묻는 문의")
                        .option("ASK_CLARIFICATION", "주문 상태와 요청이 맞지 않거나 추가 정보가 필요한 문의")
                        .build())
                .build();
        var response = decisionModel.call(prompt);
        var answer = response.getChoice("next_action");
        return new OrderContextResult(answer.getValue(), answer.getProbabilities());
    }
}
```

`LinkedHashMap`은 넣은 순서를 유지하는 Java Map입니다.<br>
작은 의사결정 모델은 필드 순서에 따라 판단이 달라질 수 있으므로, 같은 순서로 입력하기 위해 사용합니다.<br>
전송되는 `state`의 형태는 다음과 같습니다.

```json
{
  "message": "받은 상품을 반품하고 싶어요.",
  "orderStatus": "DELIVERED",
  "daysSinceDelivery": 2
}
```

`.text(...)` 대신 `.fields(...)`를 사용했으며, 선택지를 제시하고 답을 읽는 방식은 앞의 Choice와 같습니다.<br>
이 실습은 입력 구조를 확인하기 위해 주문 정보를 요청 본문으로 받습니다.<br>
실제 서비스에서는 주문 상태·배송일을 서버의 주문 조회 결과로 준비해야 합니다.<br>
`EXPLAIN_RETURN`은 반품 안내가 필요하다는 분류이며, 반품 자격 승인·접수·환불을 수행한 결과가 아닙니다.

## 실행과 확인

Ollama와 Spring 앱이 실행된 상태에서 저장소 루트의 다른 터미널에서 요청합니다.

```bash
curl -sS http://127.0.0.1:8115/api/ollama/order-context \
  -H 'Content-Type: application/json' \
  --data-raw '{"message":"받은 상품을 반품하고 싶어요.","orderStatus":"DELIVERED","daysSinceDelivery":2}'
```

고정 응답으로 요청·결과를 확인하려면 [OllamaDecisionLocalModelTest](../../../labs/15-00-ollama-decisions/src/test/java/com/example/ailearning/l15ollama/OllamaDecisionLocalModelTest.java)를 읽습니다.<br>
실제 판단은 [OllamaDecisionLiveTest](../../../labs/15-00-ollama-decisions/src/test/java/com/example/ailearning/l15ollama/OllamaDecisionLiveTest.java)로 확인합니다.<br>
실행 명령과 준비 순서는 [실습 README](../../../labs/15-00-ollama-decisions/README.md)에 있습니다.

## 기본 모델의 실제 결과 읽기

2026-11-24에 `d1-omni:600m`으로 위 한국어 반품 사례를 실행했을 때 `ASK_CLARIFICATION`이 선택됐습니다.<br>
이 예제의 기대값인 `EXPLAIN_RETURN`과 달라 관련 Live 테스트는 실패했습니다.<br>
같은 뜻의 고객 문장을 영어로 전달한 비교에서는 `EXPLAIN_RETURN`이 선택됐습니다.

API가 응답했다는 사실과 모델이 기대한 안내를 선택했다는 사실은 구분해야 합니다.<br>
기본 모델은 한국어 최적화 모델이 아니므로, 이 결과를 반품 가능 여부의 확정 판단으로 사용하지 않습니다.<br>
수치와 비교 조건은 [실제 추론 검증 기록](../../verification/15-ollama-and-ai21-upgrade.md#한국어영어-교차-확인)에서 확인할 수 있습니다.

[15단원 목차](../15-ollama-decisions.md)
