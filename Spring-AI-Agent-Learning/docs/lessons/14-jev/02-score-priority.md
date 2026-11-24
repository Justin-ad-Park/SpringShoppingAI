# 14-2. Score로 상담 우선순위 정하기

**목표:** 순서가 있는 평가 기준으로 상담 시급성을 수치화하고, 모델 점수와 서비스 우선순위를 구분합니다.

**실행 프로젝트:** [14-00-jev-decisions](../../../labs/14-00-jev-decisions/README.md).

## 어느 정도 급한 문의일까?

“텀블러 재질이 궁금해요”와 “주문 취소 마감이 오늘인데 처리가 안 됐어요”는 상담 시급성이 다릅니다.<br>
Score는 애플리케이션이 낮은 단계부터 나열한 기준으로 고객 메시지를 평가합니다.

| 단계 | 시급성 기준 |
|---|---|
| 0 | 일반 상품·정책 문의, 현재 문제나 임박한 기한 없음 |
| 1 | 배송 지연·하자 같은 현재 문제, 즉시 조치할 기한은 제시되지 않음 |
| 2 | 오늘 마감·현재 진행 중인 중복 결제 등 즉시 확인할 사정이 명시됨 |

단계 번호는 배열의 순서이며 0부터 시작합니다.<br>
세 단계의 점수 범위는 **0~2**입니다.<br>
만족도·정답 확률·퍼센트와 혼동하지 않습니다.<br>
메시지에 서술된 시급성을 평가하며 실제 주문 상태나 중복 결제를 확인하는 기능은 없습니다.

## 코드 읽기

[Lesson02SupportPriorityService](../../../labs/14-00-jev-decisions/src/main/java/com/example/ailearning/l14jev/lesson02_score/Lesson02SupportPriorityService.java)는 `Score.of(질문, 낮은 단계, 중간 단계, 높은 단계)`로 기준을 정의합니다.<br>
다음은 `assessPriority()`의 실제 발췌입니다.

```java
    public SupportPriorityResult assessPriority(String message) {
        SystemOneRequest priorityRequest = SystemOneRequest.builder()
                .state(message)
                .question("urgency", URGENCY_QUESTION)
                .build();
        SystemOneResponse priorityResponse = typeSafeClient.systemOne(priorityRequest);
        ScoreAnswer urgencyAnswer = priorityResponse.score("urgency");
        SupportPriority priority = SupportRoutingPolicy.determinePriority(urgencyAnswer.value(), urgencyAnswer.confidence());
        return new SupportPriorityResult(urgencyAnswer.value(), urgencyAnswer.confidence(), urgencyAnswer.probabilities(), priority);
    }
```

`Lesson02SupportPriorityService.assessPriority()`는 Jev 응답에서 점수·confidence·항목별 확률을 읽고, `SupportRoutingPolicy`가 정한 상담 우선순위와 함께 `SupportPriorityResult`로 반환합니다.

모델의 `score`는 단계별 확률을 반영한 값이므로 소수가 가능합니다.<br>
예를 들어 0·1·2 단계의 확률이 각각 0.1·0.2·0.7이면 `0×0.1 + 1×0.2 + 2×0.7 = 1.6`입니다.<br>
이것은 1.6점이며 80%의 정답 확률이라는 뜻은 아닙니다.

[SupportRoutingPolicy](../../../labs/14-00-jev-decisions/src/main/java/com/example/ailearning/l14jev/domain/SupportRoutingPolicy.java)가 아래 **실습용 정책**을 적용합니다.

| 조건 | 서비스의 priority |
|---|---|
| confidence < 0.7 | REVIEW: 점수가 높아도 사람의 검토 대상 |
| confidence ≥ 0.7, score ≥ 1.5 | HIGH |
| confidence ≥ 0.7, 0.5 ≤ score < 1.5 | NORMAL |
| confidence ≥ 0.7, score < 0.5 | LOW |

점수를 정하는 모델과 점수로 우선순위를 정하는 Java 코드의 책임이 다릅니다.<br>
실제 문의 큐나 상담원 배정은 이 샘플에 연결하지 않았습니다.

## 요청과 결과 관찰

```bash
curl -X POST http://localhost:8114/api/jev/priority -H 'Content-Type: text/plain; charset=UTF-8' --data '주문 취소 마감이 오늘인데 아직 처리가 안 됐어요.'
```

다음은 실제 측정값이 아닌 **응답 형태 예시**입니다.

```json
{"score":1.6,"confidence":0.8,"probabilities":{"0":0.1,"1":0.2,"2":0.7},"priority":"HIGH"}
```

Jev 원본 응답에는 단계 설명인 `legend`도 포함됩니다.<br>
이번 HTTP 응답은 코드에 정의한 고정 기준을 사용하므로 점수·확률·confidence와 정책 결과만 반환합니다.

## 검증

[Lesson02JevScoreLocalModelTest](../../../labs/14-00-jev-decisions/src/test/java/com/example/ailearning/l14jev/lesson02_score/Lesson02JevScoreLocalModelTest.java)는 순서 있는 기준 배열의 전송과 소수 점수 변환을 검사합니다.<br>
[정책 단위 테스트](../../../labs/14-00-jev-decisions/src/test/java/com/example/ailearning/l14jev/domain/SupportRoutingPolicyUnitTest.java)는 0.5·1.5·0.7 경계와 낮은 confidence의 REVIEW 우선 적용을 검증합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:14-00-jev-decisions:test --tests '*Lesson02JevScoreLocalModelTest.Score*' --tests '*SupportRoutingPolicyUnitTest'
```

실제 Jev의 판단은 [Lesson02JevScoreLiveTest](../../../labs/14-00-jev-decisions/src/test/java/com/example/ailearning/l14jev/lesson02_score/Lesson02JevScoreLiveTest.java)에서 대조 입력 두 개로 확인할 수 있습니다.<br>
[Live 기능별 선택 실행](../../../labs/14-00-jev-decisions/README.md#live-기능별-선택-실행) 명령은 실제 API를 호출하며 사용량이 발생합니다.

## 확인 질문

1. 1.6점은 평가가 잘못된 것인가요?
2. 점수 2.0, confidence 0.6이면 이 예제는 HIGH로 정하나요?

<details>
<summary>해설</summary>

1. 아닙니다.<br>
   Score는 확률을 반영한 연속 값이므로 단계 사이의 값이 나올 수 있습니다.
2. 아닙니다.<br>
   confidence가 0.7 미만이므로 REVIEW로 분류합니다.<br>
   높은 점수와 믿고 사용할 수 있는 판단인지는 별개입니다.

</details>

근거: [TypeSafe Score](https://docs.typesafe.ai/primitives/score).

[이전: Choice](01-choice-intent.md) · [14장 목차](../14-jev.md) · [다음: Noul](03-noul-human-support.md)
