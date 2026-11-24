# 15-3. Yes/No로 상담원 연결 요청 판단하기

**이번 목표:** 참일 확률을 읽고 확정하기 어려운 구간을 확인 질문으로 처리합니다.

Yes/No 질문은 “사람 상담원과 이야기하고 싶은가?”처럼 조건 하나를 묻습니다.<br>
14단원의 Noul과 같은 판단이며 Spring AI에서는 `YesNoQuestion`으로 표현합니다.<br>
Ollama에 보낼 때는 `noul` 타입으로 변환되고, `YesNoAnswer.getProbability()`는 참일 확률을 반환합니다.

## 실행 코드 읽기

[HumanSupportRequestService](../../../labs/15-00-ollama-decisions/src/main/java/com/example/ailearning/l15ollama/HumanSupportRequestService.java)의 전체 코드입니다.

```java
package com.example.ailearning.l15ollama;

import com.example.ailearning.l15ollama.domain.HumanSupportAction;
import com.example.ailearning.l15ollama.domain.SupportRoutingPolicy;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionPrompt;
import org.springframework.ai.decision.question.YesNoQuestion;
import org.springframework.stereotype.Service;

/** 15-3: 사람 상담원 연결을 원하는지 참일 확률로 판단한다. */
@Service
public class HumanSupportRequestService {
    public record HumanSupportResult(double yesProbability, HumanSupportAction action) {}

    static final YesNoQuestion HUMAN_SUPPORT_QUESTION = YesNoQuestion.builder()
            .text("고객이 사람 상담원 연결을 요청하나요?")
            .whenYes("사람 상담원과 직접 대화하고 싶다는 요청")
            .whenNo("사람 상담원 연결을 요청하지 않거나 자동 안내를 원하는 문의")
            .build();
    private final DecisionModel decisionModel;

    public HumanSupportRequestService(DecisionModel decisionModel) {
        this.decisionModel = decisionModel;
    }

    public HumanSupportResult assessHumanSupportRequest(String message) {
        var prompt = DecisionPrompt.builder()
                .text(message)
                .question("human_support", HUMAN_SUPPORT_QUESTION)
                .build();
        var response = decisionModel.call(prompt);
        var answer = response.getYesNo("human_support");
        return new HumanSupportResult(answer.getProbability(),
                SupportRoutingPolicy.determineHumanSupportAction(answer.getProbability()));
    }
}
```

`whenYes`·`whenNo`는 각 판단에 해당하는 상황을 설명합니다.<br>
`isYes()`로 단순히 나눌 수도 있지만, 이 예제는 중간 확률에서 확인 질문을 하도록 원래 확률을 정책에 넘깁니다.

| 참일 확률 | Java가 반환하는 안내 방식 |
|---|---|
| 0.2 이하 | `CONTINUE_CHAT`: 자동 안내 계속 |
| 0.2 초과, 0.8 미만 | `ASK_CLARIFICATION`: 연결을 원하는지 확인 |
| 0.8 이상 | `OFFER_HUMAN_SUPPORT`: 상담원 연결 안내 |

0.5는 “절반만 연결을 원함”이 아니라 참·거짓 판단이 불확실하다는 뜻입니다.<br>
이 질문은 별도 confidence를 반환하지 않습니다.<br>
`OFFER_HUMAN_SUPPORT`는 안내 방식의 이름이며 실제 상담원 연결 API는 실행하지 않습니다.

## 실행과 확인

Ollama와 Spring 앱이 실행된 상태에서 저장소 루트의 다른 터미널에서 요청합니다.

```bash
curl -sS http://127.0.0.1:8115/api/ollama/human-support \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '자동 응답 말고 사람 상담원과 이야기하고 싶어요.'
```

고정 응답으로 요청·결과를 확인하려면 [OllamaDecisionLocalModelTest](../../../labs/15-00-ollama-decisions/src/test/java/com/example/ailearning/l15ollama/OllamaDecisionLocalModelTest.java)를 읽습니다.<br>
실제 판단은 [OllamaDecisionLiveTest](../../../labs/15-00-ollama-decisions/src/test/java/com/example/ailearning/l15ollama/OllamaDecisionLiveTest.java)로 확인합니다.<br>
실행 명령과 준비 순서는 [실습 README](../../../labs/15-00-ollama-decisions/README.md)에 있습니다.

[15단원 목차](../15-ollama-decisions.md)
