# 15-5. 여러 질문을 한 번에 보내기

**이번 목표:** 한 고객 메시지에 대한 서로 다른 질문을 한 번의 모델 요청으로 전달합니다.

앞 수업에서는 의도·상담원 요청·시급성을 각각 호출했습니다.<br>
이번에는 같은 고객 문장에 세 질문을 넣고 `call(...)`은 한 번만 실행합니다.<br>
모델이 앞 질문의 답을 보고 다음 질문을 선택하는 Agent 반복과는 다릅니다.<br>
개발자가 미리 정한 세 질문을 한 요청에 담습니다.

## 실행 코드 읽기

[Lesson05CustomerAssessmentService](../../../labs/15-00-ollama-decisions/src/main/java/com/example/ailearning/l15ollama/lesson05_multiple/Lesson05CustomerAssessmentService.java)의 전체 코드입니다.

```java
package com.example.ailearning.l15ollama.lesson05_multiple;

import com.example.ailearning.l15ollama.lesson02_choice.Lesson02IntentClassificationService;
import com.example.ailearning.l15ollama.lesson03_yesno.Lesson03HumanSupportRequestService;
import com.example.ailearning.l15ollama.lesson04_score.Lesson04SupportPriorityService;

import com.example.ailearning.l15ollama.domain.ShoppingIntent;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionPrompt;
import org.springframework.stereotype.Service;

/** 15-5: 앞에서 배운 세 질문을 한 번의 모델 요청으로 보낸다. */
@Service
public class Lesson05CustomerAssessmentService {
    public record CustomerAssessment(ShoppingIntent intent, double humanSupportProbability, double urgencyScore) {}
    private final DecisionModel decisionModel;

    public Lesson05CustomerAssessmentService(DecisionModel decisionModel) {
        this.decisionModel = decisionModel;
    }

    public CustomerAssessment assessCustomerMessage(String message) {
        var prompt = DecisionPrompt.builder()
                .text(message)
                .question("intent", Lesson02IntentClassificationService.INTENT_QUESTION)
                .question("human_support", Lesson03HumanSupportRequestService.HUMAN_SUPPORT_QUESTION)
                .question("urgency", Lesson04SupportPriorityService.URGENCY_QUESTION)
                .build();
        var response = decisionModel.call(prompt);
        return new CustomerAssessment(ShoppingIntent.valueOf(response.getChoice("intent").getValue()),
                response.getYesNo("human_support").getProbability(), response.getScore("urgency").getValue());
    }
}
```

세 Service의 질문 상수는 앞 수업에서 읽은 선택지와 기준을 그대로 재사용합니다.<br>
`getChoice("intent")`·`getYesNo("human_support")`·`getScore("urgency")`처럼 **질문 이름과 답 타입**을 맞춰 결과를 읽습니다.<br>
로컬 테스트는 전송한 HTTP 요청이 한 개이고 그 안의 질문이 세 개인지 검사합니다.<br>
질문 세 개의 확률을 곱하거나 하나의 신뢰도로 합산하는 기능은 아닙니다.

## 실행과 확인

Ollama와 Spring 앱이 실행된 상태에서 저장소 루트의 다른 터미널에서 요청합니다.

```bash
curl -sS http://127.0.0.1:8115/api/ollama/assessment \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '배송이 지연되고 있어요. 오늘 꼭 받아야 하니 사람 상담원에게 연결해 주세요.'
```

고정 응답으로 요청·결과를 확인하려면 [Lesson05CustomerAssessmentLocalModelTest](../../../labs/15-00-ollama-decisions/src/test/java/com/example/ailearning/l15ollama/lesson05_multiple/Lesson05CustomerAssessmentLocalModelTest.java)를 읽습니다.<br>
실제 판단은 [Lesson05CustomerAssessmentLiveTest](../../../labs/15-00-ollama-decisions/src/test/java/com/example/ailearning/l15ollama/lesson05_multiple/Lesson05CustomerAssessmentLiveTest.java)로 확인합니다.<br>
실행 명령과 준비 순서는 [실습 README](../../../labs/15-00-ollama-decisions/README.md)에 있습니다.

[15단원 목차](../15-ollama-decisions.md)
