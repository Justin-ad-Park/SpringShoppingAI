# 15-2. Choice로 쇼핑 의도 분류하기

**이번 목표:** 선택지와 설명을 모델에 전달하고, 선택 결과를 실제 서비스 처리와 구분합니다.

14단원의 Choice와 같은 문제입니다.<br>
“3만원 이하 텀블러 추천”은 상품 검색, “이미 주문한 상품은 어디 있나요?”는 배송 문의로 분류합니다.<br>
이 단계에서는 한 요청에 의도 질문 하나만 보냅니다.

| Spring AI 타입 | 역할 |
|---|---|
| `DecisionModel` | 판단 요청을 보내는 인터페이스. 이 실습의 구현은 `OllamaDecisionModel` |
| `DecisionPrompt` | 판단할 입력과 질문을 담은 요청 |
| `ChoiceQuestion` | 질문 문장과 선택지별 이름·설명 |
| `DecisionResponse` | 질문 이름별 답을 담은 응답 |
| `ChoiceAnswer` | 선택된 항목, 항목별 확률, confidence |


## 실행 코드 읽기

[IntentClassificationService](../../../labs/15-00-ollama-decisions/src/main/java/com/example/ailearning/l15ollama/IntentClassificationService.java)의 전체 코드입니다.

```java
package com.example.ailearning.l15ollama;

import com.example.ailearning.l15ollama.domain.ShoppingIntent;
import com.example.ailearning.l15ollama.domain.SupportRoutingPolicy;
import java.util.Map;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionPrompt;
import org.springframework.ai.decision.question.ChoiceQuestion;
import org.springframework.stereotype.Service;

/** 15-2: 정해진 쇼핑 의도 중 하나를 선택한다. */
@Service
public class IntentClassificationService {
    public record IntentResult(ShoppingIntent predictedIntent, Double confidence,
                               Map<String, Double> probabilities, boolean clarificationRequired) {}

    static final ChoiceQuestion INTENT_QUESTION = ChoiceQuestion.builder()
            .text("고객 메시지의 주된 쇼핑 의도를 하나 선택하세요.")
            .option("PRODUCT_SEARCH", "상품 검색, 추천, 상품 특징이나 가격 문의")
            .option("ORDER_DELIVERY", "이미 주문한 상품의 주문 상태 또는 배송 조회")
            .option("RETURN_EXCHANGE", "구매한 상품의 반품 또는 교환 문의")
            .option("OTHER", "위 항목에 해당하지 않거나 여러 의도가 섞인 요청")
            .build();
    private final DecisionModel decisionModel;

    public IntentClassificationService(DecisionModel decisionModel) {
        this.decisionModel = decisionModel;
    }

    public IntentResult classifyIntent(String message) {
        var prompt = DecisionPrompt.builder()
                .text(message)
                .question("intent", INTENT_QUESTION)
                .build();
        var response = decisionModel.call(prompt);
        var answer = response.getChoice("intent");
        var intent = ShoppingIntent.valueOf(answer.getValue());
        return new IntentResult(intent, answer.getConfidence(), answer.getProbabilities(),
                SupportRoutingPolicy.requiresIntentClarification(intent, answer.getConfidence()));
    }
}
```

`text(message)`는 판단할 고객 메시지이고, `question("intent", ...)`의 `intent`는 응답을 찾을 때 사용할 이름입니다.<br>
`call(prompt)`가 실제 로컬 Ollama를 호출하고, `getChoice("intent")`로 해당 질문의 답을 읽습니다.<br>
각 `option`의 첫 인자는 Java에서 처리할 이름, 두 번째 인자는 모델이 그 항목을 선택할 상황입니다.

[SupportRoutingPolicy](../../../labs/15-00-ollama-decisions/src/main/java/com/example/ailearning/l15ollama/domain/SupportRoutingPolicy.java)는 `OTHER`이거나 confidence가 0.7 미만·없음이면 확인이 필요하다고 표시합니다.<br>
0.7은 이 실습의 기준이며 Ollama의 기본 설정이 아닙니다.<br>
선택됐다는 이유로 실제 상품 검색이나 주문 변경을 실행하지 않습니다.

예를 들어 `predictedIntent=PRODUCT_SEARCH`, `confidence=0.85`, `clarificationRequired=false`이면 상품 검색 의도로 판단했고 이 예제의 확인 기준을 통과한 것입니다.<br>
분류가 반드시 정답이라는 뜻은 아닙니다.

## 실행과 확인

Ollama와 Spring 앱이 실행된 상태에서 저장소 루트의 다른 터미널에서 요청합니다.

```bash
curl -sS http://127.0.0.1:8115/api/ollama/intent \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '3만원 이하 텀블러를 추천해 주세요.'
```

고정 응답으로 요청·결과를 확인하려면 [OllamaDecisionLocalModelTest](../../../labs/15-00-ollama-decisions/src/test/java/com/example/ailearning/l15ollama/OllamaDecisionLocalModelTest.java)를 읽습니다.<br>
실제 판단은 [OllamaDecisionLiveTest](../../../labs/15-00-ollama-decisions/src/test/java/com/example/ailearning/l15ollama/OllamaDecisionLiveTest.java)로 확인합니다.<br>
실행 명령과 준비 순서는 [실습 README](../../../labs/15-00-ollama-decisions/README.md)에 있습니다.

[15단원 목차](../15-ollama-decisions.md)
