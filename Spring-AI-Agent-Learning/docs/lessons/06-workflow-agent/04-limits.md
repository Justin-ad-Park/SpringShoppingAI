# 06-4. 보충: 호출 상한과 실패 처리

**실행 코드·설정·테스트:** [labs/06-workflow-agent](../../../labs/06-workflow-agent/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

30~45분을 기준으로 한 번에 한 가지 흐름만 읽습니다. 단원 전체 시간은 목차의 기존 예상 시간에 포함됩니다.

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

[WorkflowAgentContractTest](../../../labs/06-workflow-agent/src/test/java/com/example/ailearning/l06decisions/WorkflowAgentContractTest.java)에서 반복 도구 선택·모델 요청 상한·느린 응답을 고정 서버로 재현합니다.
상한을 넘기면 HTTP 통신 자체는 200이고 본문의 status가 TOOL_LIMIT/MODEL_LIMIT/TIMEOUT입니다. COMPLETED와 구분하세요.
빈 질문은 Controller에서 400입니다. 이 실습에는 이전 대화 기억이 없으며 요청마다 기록과 한도가 초기화됩니다.

## 확인 질문

1. Workflow가 조건 추출에 모델을 썼다면 전체가 자율 Agent일까요?
2. Agent 프롬프트에 순서를 적으면 코드가 그 순서를 강제한 것과 같을까요?
3. 최상위 호출 1회인데 modelCalls가 4인 이유는 무엇인가요?
4. 예산 준수·가격 계산·도구 실행 상한을 코드로 확인해야 하는 이유는 무엇인가요?

<details><summary>해설</summary>

다음 행동을 Java의 미리 정한 경로가 선택하면 Workflow입니다. 프롬프트는 행동 지시이고 코드의 순서 보장과 다릅니다.
도구 결과를 받은 모델이 다시 판단하므로 여러 모델 요청이 발생합니다. 정확한 계산·실행 제한은 모델 문장의 준수 여부와 별도로 강제해야 합니다.
호출 기록에서 각 판단 주체를 찾고 같은 요구에 맞는 방식을 선택하면 완료입니다.
</details>

보충: [공식 Agent 패턴](https://docs.spring.io/spring-ai/reference/api/effective-agents.html), [Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html).

## 여기서 멈춰 확인하기

코드를 닫고 “이번 입력 → 새로 하는 처리 → 확인할 결과”를 한 문장으로 적어 보세요.
설명하기 어렵다면 위 핵심 메서드만 다시 읽습니다. 테스트용 HTTP 서버 구현을 이해해야 다음으로 갈 수 있는 것은 아닙니다.

[이전](03-hybrid.md) · [다음](../06-workflow-agent.md) · [단원 목차](../06-workflow-agent.md) · [학습 방법](../../learning-guide.md)
