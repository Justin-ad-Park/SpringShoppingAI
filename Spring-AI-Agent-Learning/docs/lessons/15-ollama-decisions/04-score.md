# 15-4. Score로 상담 시급성 평가하기

**이번 목표:** 순서가 있는 기준으로 평가하고 소수 점수와 대표 단계를 구분합니다.

Score는 서로 다른 분야를 고르는 Choice와 달리 **낮음부터 높음까지 순서가 있는 기준**을 사용합니다.<br>
세 기준을 주면 0·1·2단계가 되며, 모델은 단계별 확률을 반영한 소수 점수를 반환할 수 있습니다.

## 실행 코드 읽기

[SupportPriorityService](../../../labs/15-00-ollama-decisions/src/main/java/com/example/ailearning/l15ollama/SupportPriorityService.java)의 전체 코드입니다.

```java
package com.example.ailearning.l15ollama;

import com.example.ailearning.l15ollama.domain.SupportPriority;
import com.example.ailearning.l15ollama.domain.SupportRoutingPolicy;
import java.util.List;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionPrompt;
import org.springframework.ai.decision.question.ScoreQuestion;
import org.springframework.stereotype.Service;

/** 15-4: 순서가 있는 세 기준으로 시급성을 평가한다. */
@Service
public class SupportPriorityService {
    public record PriorityResult(double score, int nearestLevel, Double confidence,
                                 List<Double> probabilities, SupportPriority priority) {}

    static final ScoreQuestion URGENCY_QUESTION = ScoreQuestion.of("얼마나 시급한 고객 문의인가요?",
            "일반 문의: 시간 제약이나 현재 문제가 없는 정보 문의",
            "현재 문제: 고객이 불편을 겪지만 임박한 마감은 없음",
            "즉시 확인 필요: 오늘 마감이나 즉시 처리해야 할 문제가 있음");
    private final DecisionModel decisionModel;

    public SupportPriorityService(DecisionModel decisionModel) {
        this.decisionModel = decisionModel;
    }

    public PriorityResult assessPriority(String message) {
        var prompt = DecisionPrompt.builder()
                .text(message)
                .question("urgency", URGENCY_QUESTION)
                .build();
        var response = decisionModel.call(prompt);
        var answer = response.getScore("urgency");
        return new PriorityResult(answer.getValue(), answer.getNearestLevel(), answer.getConfidence(),
                answer.getProbabilities(), SupportRoutingPolicy.determinePriority(answer.getValue(), answer.getConfidence()));
    }
}
```

예를 들어 단계별 확률이 `[0.1, 0.2, 0.7]`이면 점수는 `0×0.1 + 1×0.2 + 2×0.7 = 1.6`입니다.

| 반환값 | 의미 |
|---|---|
| `score` | 확률을 반영한 연속 점수. 이 예에서는 0~2 |
| `nearestLevel` | `getNearestLevel()`이 고른 확률이 가장 높은 대표 단계. 위 예에서는 2 |
| `confidence` | 모델의 확률 분포에 대한 값. 실제 정답률 보증이 아님 |
| `priority` | Java 정책이 정한 처리 우선순위 |

`getNearestLevel()`은 단순히 소수 점수를 반올림하는 메서드가 아닙니다.<br>
가장 높은 확률의 단계가 여러 개이면 점수와 가까운 단계를 고릅니다.<br>
대표 단계와 서비스의 우선순위도 구분합니다.<br>
이 예제는 confidence가 0.7 미만·없음이면 `REVIEW`, 나머지는 점수 0.5 미만 `LOW`, 0.5 이상 `NORMAL`, 1.5 이상 `HIGH`로 처리합니다.

## 실행과 확인

Ollama와 Spring 앱이 실행된 상태에서 저장소 루트의 다른 터미널에서 요청합니다.

```bash
curl -sS http://127.0.0.1:8115/api/ollama/priority \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '취소 마감이 오늘 한 시간 뒤인데 아직 처리가 안 됐어요.'
```

고정 응답으로 요청·결과를 확인하려면 [OllamaDecisionLocalModelTest](../../../labs/15-00-ollama-decisions/src/test/java/com/example/ailearning/l15ollama/OllamaDecisionLocalModelTest.java)를 읽습니다.<br>
실제 판단은 [OllamaDecisionLiveTest](../../../labs/15-00-ollama-decisions/src/test/java/com/example/ailearning/l15ollama/OllamaDecisionLiveTest.java)로 확인합니다.<br>
실행 명령과 준비 순서는 [실습 README](../../../labs/15-00-ollama-decisions/README.md)에 있습니다.

[15단원 목차](../15-ollama-decisions.md)
