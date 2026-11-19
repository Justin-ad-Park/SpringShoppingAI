# 12-1. 고정 사례와 품질 평가의 기준

**실행 코드·설정·테스트:** [labs/12-00-evaluation](../../../labs/12-00-evaluation/README.md).<br>
저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 고정된 질문과 기대 결과를 정하고 실제 답변과 비교하는 방법을 이해합니다.

**코드 읽기 안내:** RuleBasedEvaluator의 기대 값 비교가 핵심입니다.<br>
점수가 높아도 사례가 좁으면 실제 품질을 대표하지 못합니다.

## 답변을 비교할 기준 정하기

같은 질문을 모델에 보내고, 미리 정한 기대 상태·출처·핵심 문구와 실제 결과를 비교합니다.<br>
이번에는 우선 사람이 읽을 수 있는 **규칙 기반 평가**를 만듭니다.

| 확인 항목 | 이번 구현 | 한계 |
|---|---|---|
| 상태 | 기대 상태와 실제 상태 비교 | ANSWERED 자체가 사실성 보장은 아님 |
| 출처 | 기대 ID가 실제 출처 목록에 포함되는지 | 인용과 주장 전체의 일치를 증명하지 못함 |
| 핵심 문구 | 3000, 7일 같은 기대 문자열 포함 | 표현이 달라지면 오판, “7일이 아니다”도 포함 검사 통과 가능 |

로컬 고정 응답 테스트는 이 비교 코드의 동작을 재현하며, 실제 모델 평가는 실제 응답의 품질을 확인합니다.<br>
한 번 답이 맞았다고 모든 입력의 품질이 보장되지는 않습니다.<br>
문구 검사만으로 평가를 끝내지 말고 답변과 출처 원문을 사람이 읽는 절차도 둡니다.<br>
이번 소수업은 Java 규칙으로 결과를 비교합니다.<br>
다음 [12-2](02-model-judge.md)에서는 모델을 평가자로 사용하는 LLM-as-a-judge를 배웁니다.

## 같은 기준으로 답변을 비교하기

“미개봉 반품 기간은?”이라는 질문을 예로 들면, 사람은 먼저 정책 원문에서 기대 기준을 정합니다.<br>
평가기가 그 기준을 모델 답변에서 스스로 만들어 내도록 맡기지 않습니다.

| 비교 대상 | 기대 기준 | 후보 답변 A | 후보 답변 B |
|---|---|---|---|
| 업무 상태 | `ANSWERED` | `ANSWERED` | `ANSWERED` |
| 출처 ID | `return-sealed` 포함 | 포함 | 포함 |
| 필수 문구 | `7일` 포함 | “반품은 7일 이내입니다.” | “반품은 30일 이내입니다.” |
| 이 규칙의 결과 | 모두 만족하면 통과 | 실패 사유 없음 | 필수 문구 누락 |

출처가 같더라도 답변 내용은 다를 수 있습니다.<br>
후보 A도 이 규칙을 통과한 것이지, 미개봉·수령 후 같은 모든 적용 조건을 완전하게 설명했음을 보장하지는 않습니다.

## 준비된 답변을 규칙으로 평가하기

먼저 [RuleBasedEvaluator](../../../labs/12-00-evaluation/src/main/java/com/example/ailearning/l12evaluation/rules/RuleBasedEvaluator.java)를 읽습니다.<br>
기대 상태·출처·필수 문구와 준비된 후보 답변을 비교합니다.<br>
이 클래스는 Spring이나 모델 없이 실행됩니다.

```java
package com.example.ailearning.l12evaluation.rules;

import java.util.ArrayList;
import java.util.List;

/** 12-1: 준비된 답변을 기대값과 비교한다. 답변 생성·검색·계측은 하지 않는다. */
public class RuleBasedEvaluator {
    public record ExpectedAnswer(String status, List<String> sourceIds, String requiredText) {}
    public record CandidateAnswer(String status, List<String> sourceIds, String text) {}

    public List<String> evaluate(ExpectedAnswer expected, CandidateAnswer actual) {
        var failures = new ArrayList<String>();
        if (!actual.status().equals(expected.status())) failures.add("상태 불일치");
        if (!actual.sourceIds().containsAll(expected.sourceIds())) failures.add("expected source ID missing");
        if (!actual.text().contains(expected.requiredText())) failures.add("expected text missing");
        return List.copyOf(failures);
    }
}
```

`ExpectedAnswer`는 사람이 정한 기대 기준, `CandidateAnswer`는 검사할 답변을 담습니다.<br>
`evaluate()`는 상태를 비교하고, `containsAll()`로 필요한 출처가 모두 있는지, `contains()`로 필수 문구가 있는지 검사합니다.<br>
실패한 항목의 설명을 목록에 추가하며 목록이 비면 정한 규칙을 통과한 것입니다.

## 실행과 결과

[RuleBasedEvaluatorUnitTest](../../../labs/12-00-evaluation/src/test/java/com/example/ailearning/l12evaluation/rules/RuleBasedEvaluatorUnitTest.java)는 7일 답변의 통과와 30일 답변의 문구 실패를 출력하고 검증합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:12-00-evaluation:test --tests '*RuleBasedEvaluatorUnitTest'
```

테스트는 후보 A에 빈 목록 `[]`, 후보 B에 `[expected text missing]`을 기대합니다.<br>
이 문자열은 샘플이 정의한 실패 사유이며 Spring AI 표준 평가 코드가 아닙니다.

실패 사유 목록이 비면 정한 규칙을 통과한 것입니다.<br>
문장의 의미까지 보증하지는 않습니다.<br>
모델이 만든 실제 답변을 이 규칙으로 검사하는 [EvaluationRunner](../../../labs/12-00-evaluation/src/main/java/com/example/ailearning/l12evaluation/EvaluationRunner.java)는 [12-5](05-failures.md)에서 검색·생성·계측과 결합합니다.

## 확인 질문

핵심 문구 포함 검사가 틀린 답변도 통과시킬 수 있는 예를 들어 보세요.

<details>
<summary>해설</summary>

예를 들어 기대 문구가 “7일”이면 “반품은 7일이 아니라 30일입니다.”라는 잘못된 답도 `contains("7일")`을 통과합니다.<br>
문구 포함 검사는 문장의 의미가 맞는지까지 확인하지 않습니다.

</details>

[이전](../12-evaluation.md) · [다음](02-model-judge.md) · [단원 목차](../12-evaluation.md) · [학습 방법](../../learning-guide.md)
