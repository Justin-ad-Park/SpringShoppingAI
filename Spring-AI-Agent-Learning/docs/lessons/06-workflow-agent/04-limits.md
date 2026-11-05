# 06-4. 보충: 호출 상한과 실패 처리

**실행 코드·설정·테스트:** [labs/06-workflow-agent](../../../labs/06-workflow-agent/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 무한 반복과 긴 대기를 막는 코드의 위치를 확인합니다.

**코드 읽기 안내:** 처음에는 설정과 중단 상태만 관찰하세요. Future·Executor·CountedChatModel의 내부 구현은 Java 동시성 보충 읽기입니다. 기본 비교의 선수 지식이 아닙니다.

## 실행 상한은 코드로 강제한다

`application.yml`은 기본값으로 모델 요청 8회, 도구 실행 4회, 전체 요청 대기 10000ms를 지정합니다.
프롬프트에 “4번만 실행”이라고 적는 방식이 아닙니다.

- [support/RunState](../../../labs/06-workflow-agent/src/main/java/com/example/ailearning/l06decisions/support/RunState.java)는 다음 요청/도구 실행 전 횟수·마감 시간을 확인합니다.
- [support/CountedChatModel](../../../labs/06-workflow-agent/src/main/java/com/example/ailearning/l06decisions/support/CountedChatModel.java)은 실제 모델 호출을 위임하면서 기록합니다. 별도 Advisor를 구현하지 않습니다.
- 서비스의 `run()`은 별도 작업을 정해진 시간만 기다리고, 초과 시 취소해 TIMEOUT을 반환합니다. 늦은 응답 뒤에는 새 도구/모델 호출을 시작하지 못하게 합니다.
- 취소가 외부 모델 서버의 계산·과금 중단까지 보장하지는 않습니다. 요청별 SDK 타임아웃도 5초로 설정했습니다. 이 단원의 도구는 빠른 로컬 읽기 전용 기능입니다.

이 보조 코드는 비교 결과를 재현하기 위한 장치입니다. 먼저 `extract → workflow → agent → hybrid`를 읽고 필요할 때 support를 읽으세요.
Thread/Future의 상세 학습을 Spring AI 입문의 선수 지식으로 요구하지 않습니다.

[WorkflowAgentLocalModelTest](../../../labs/06-workflow-agent/src/test/java/com/example/ailearning/l06decisions/WorkflowAgentLocalModelTest.java)에서 반복 도구 선택·모델 요청 상한·느린 응답을 고정 서버로 재현합니다.
상한을 넘기면 HTTP 통신 자체는 200이고 본문의 status가 TOOL_LIMIT/MODEL_LIMIT/TIMEOUT입니다. COMPLETED와 구분하세요.
빈 질문은 Controller에서 400입니다. 이 실습에는 이전 대화 기억이 없으며 요청마다 기록과 한도가 초기화됩니다.

## 호출 기록용 클라이언트 (보충)

여기서 `client(run)`은 [ComparisonService의 도우미 메서드](../../../labs/06-workflow-agent/src/main/java/com/example/ailearning/l06decisions/ComparisonService.java)입니다.
앞 단원처럼 Builder를 주입받는 대신, 이번에는 주입받은 `ChatModel`을 호출 기록용 `CountedChatModel`로 감싸 ChatClient를 만듭니다.
`run.startClient()`는 이 생성 위치를 세므로, 이 실습에서는 그 수가 최상위 호출 수와 같습니다. ChatClient 생성 자체가 모델 요청은 아닙니다.

```java
private ChatClient client(RunState run) {
    run.startClient();
    return ChatClient.create(new CountedChatModel(model, run));
}
```

`RunState`와 `CountedChatModel`은 횟수·시간을 확인하는 제공된 보조 코드입니다. 필요할 때 위 소스 링크를 따라 읽습니다.

## 확인 질문

1. 모델 요청 상한과 도구 실행 상한은 각각 어떤 보조 코드가 검사하나요?
2. 응답 본문의 `TIMEOUT`은 외부 모델의 과금 중단까지 보장하나요?
3. HTTP 200 응답에서 실행 상한으로 중단된 것을 어떻게 구분하나요?

<details><summary>해설</summary>

`CountedChatModel`이 모델 호출을 기록하고 `RunState`가 모델·도구의 다음 실행 전 한도를 검사합니다.
시간을 초과하면 작업 취소를 요청하지만 제공 업체의 계산·과금 중단까지 보장하지 않습니다.
HTTP 상태와 별개로 본문의 `MODEL_LIMIT`, `TOOL_LIMIT`, `TIMEOUT`을 확인합니다.
</details>

보충: [공식 Agent 패턴](https://docs.spring.io/spring-ai/reference/api/effective-agents.html), [Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html).

## 여기서 멈춰 확인하기

모델 요청 상한·도구 실행 상한·시간 제한을 각각 어디서 검사하는지 연결해 보세요.

[이전](03-hybrid.md) · [다음](../06-workflow-agent.md) · [단원 목차](../06-workflow-agent.md) · [학습 방법](../../learning-guide.md)
