# 14-3. Noul로 상담원 연결 요청 판별하기

**목표:** 특정 조건이 참일 확률을 읽고, 불확실한 구간을 별도로 처리합니다.

**실행 프로젝트:** [14-00-jev-decisions](../../../labs/14-00-jev-decisions/README.md).

## 참·거짓을 바로 boolean으로 받지 않는 이유

“자동 응답 말고 사람 상담원과 이야기하고 싶어요”에서 확인할 조건은 **고객이 상담원 연결을 요청했는가**입니다. Noul은 이 조건이 참일 확률을 0~1로 반환합니다.

0에 가까우면 아니라고 판단하고 1에 가까우면 그렇다고 판단합니다. 0.5는 연결을 “절반만 원한다”는 뜻이 아니라 참·거짓 판단이 불확실하다는 뜻입니다. 별도 confidence 필드는 없습니다.

## 코드 읽기

[HumanSupportRequestService](../../../labs/14-00-jev-decisions/src/main/java/com/example/ailearning/l14jev/HumanSupportRequestService.java)의 질문 정의입니다.

```java
private static final Noul HUMAN_SUPPORT_QUESTION = Noul.builder()
        .instructions("고객이 자동 응답 대신 사람 상담원과의 연결을 요청하고 있다.")
        .whenTrue("사람 상담원과 통화하거나 채팅하고 싶다는 명시적인 요청이 있음")
        .whenFalse("단순 문의이거나 상담원 연결을 원하지 않는다고 말함")
        .build();
```

`instructions`는 판단할 명제, `whenTrue`·`whenFalse`는 참·거짓의 구분 기준입니다. 호출에는 질문 ID `human_support`를 사용하고, `SystemOneResponse`에서 `NoulAnswer`를 읽고 `value()`로 참일 확률을 얻습니다. 서비스는 확률과 정책의 처리 경로를 담은 `HumanSupportRequestResult`를 반환합니다. 원본 응답의 필드명은 `noul`이며, 고객용 API에서는 의미가 드러나도록 `yesProbability`로 반환합니다.

다음은 실제 `assessHumanSupportRequest()` 메서드입니다.

```java
    public HumanSupportRequestResult assessHumanSupportRequest(String message) {
        SystemOneRequest humanSupportRequest = SystemOneRequest.builder()
                .state(message)
                .question("human_support", HUMAN_SUPPORT_QUESTION)
                .build();
        SystemOneResponse humanSupportResponse = typeSafeClient.systemOne(humanSupportRequest);
        NoulAnswer humanSupportAnswer = humanSupportResponse.noul("human_support");
        double yesProbability = humanSupportAnswer.value();
        HumanSupportAction action = SupportRoutingPolicy.determineHumanSupportAction(yesProbability);
        return new HumanSupportRequestResult(yesProbability, action);
    }
```

[SupportRoutingPolicy](../../../labs/14-00-jev-decisions/src/main/java/com/example/ailearning/l14jev/domain/SupportRoutingPolicy.java)의 실습용 분기 기준은 다음과 같습니다.

| yesProbability | action | 의미 |
|---|---|---|
| 0~0.2 | CONTINUE_CHAT | 자동 상담을 계속하는 경로 |
| 0.2 초과~0.8 미만 | ASK_CLARIFICATION | 상담원 연결을 원하는지 확인 |
| 0.8~1 | OFFER_HUMAN_SUPPORT | 상담원 연결 안내를 제시하는 경로 |

0.2·0.8은 Jev가 강제하는 값이 아닙니다. 모델이 분류하고 Java가 정책을 적용합니다. 이 샘플은 action을 반환하며 실제 확인 질문 문장이나 연결 화면·상담 시스템을 구현하지 않습니다. 실제 서비스에서는 명시적인 연결 요청을 우선 처리하는 규칙과 실패 시 안내도 검토해야 합니다.

## 요청과 결과 관찰

```bash
curl -X POST http://localhost:8114/api/jev/human-support -H 'Content-Type: text/plain; charset=UTF-8' --data '자동 응답 말고 사람 상담원과 이야기하고 싶어요.'
```

다음은 실제 측정값이 아닌 **응답 형태 예시**입니다.

```json
{"yesProbability":0.9,"action":"OFFER_HUMAN_SUPPORT"}
```

다른 명제로 Noul을 사용하려면 무엇의 확률인지부터 정해야 합니다. “상담원 연결 요청”의 확률과 “반품 가능”의 확률은 다른 의미입니다. 반품 자격·권한·금액 같은 확정 규칙은 정책 데이터와 Java 검증으로 판단합니다.

## 검증

[JevLocalModelTest](../../../labs/14-00-jev-decisions/src/test/java/com/example/ailearning/l14jev/JevLocalModelTest.java)는 Noul 요청 기준과 응답 변환, 0.9와 0.5에서의 분기를 검사합니다. [정책 단위 테스트](../../../labs/14-00-jev-decisions/src/test/java/com/example/ailearning/l14jev/domain/SupportRoutingPolicyUnitTest.java)는 0.2·0.8 양쪽 경계를 확인합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:14-00-jev-decisions:test --tests '*JevLocalModelTest.Noul*' --tests '*SupportRoutingPolicyUnitTest'
```

실제 Jev의 판단은 [JevLiveTest](../../../labs/14-00-jev-decisions/src/test/java/com/example/ailearning/l14jev/JevLiveTest.java)에서 대조 입력 두 개로 확인할 수 있습니다. [Live 기능별 선택 실행](../../../labs/14-00-jev-decisions/README.md#live-기능별-선택-실행) 명령은 실제 API를 호출하며 사용량이 발생합니다.

## 확인 질문

1. Noul의 0.5를 상담원 연결 요구 강도의 중간값으로 읽어도 되나요?
2. 0.79와 0.8은 이 예제에서 어떻게 다르게 처리하나요?

<details>
<summary>해설</summary>

1. 아닙니다. 연결 요청이라는 명제가 참일 확률입니다. 요구 강도나 시급성의 정도를 평가하려면 순서 있는 Score 기준이 필요합니다.
2. 0.79는 ASK_CLARIFICATION, 0.8은 OFFER_HUMAN_SUPPORT입니다. 경계를 포함하는지는 Java 정책과 단위 테스트에 명시돼 있습니다.

</details>

근거: [TypeSafe Noul](https://docs.typesafe.ai/primitives/noul).

[이전: Score](02-score-priority.md) · [14장 목차](../14-jev.md) · [전체 교육 계획](../../curriculum.md)
