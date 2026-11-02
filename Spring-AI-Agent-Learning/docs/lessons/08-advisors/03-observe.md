# 08-3. 순서와 호출 횟수 실험

30~45분을 기준으로 한 번에 한 가지 흐름만 읽습니다. 단원 전체 시간은 목차의 기존 예상 시간에 포함됩니다.

**이번 목표:** 로그의 반복과 중복 저장 여부를 관찰합니다.

**코드 읽기 안내:** 한 번에 옵션 하나만 바꾸고 실행 기록을 비교합니다. 앞서 배운 메모리 저장 순서가 기준입니다.

## 실행

파일 읽기: TraceAdvisor → AdvisorChatService → [PriceTools](../../../labs/08-advisors/src/main/java/com/example/ailearning/l08advisors/PriceTools.java) → [AdvisorController](../../../labs/08-advisors/src/main/java/com/example/ailearning/l08advisors/AdvisorController.java).
PriceTools는 05에서 배운 @Tool 방식으로 가상 허브티 가격 12000원을 조회하는 작은 도구입니다.
명령은 저장소 루트에서 실행합니다. bootRun 전 [01장 환경 변수](../01-setup.md#4-실제-모델을-사용할-때의-환경-변수)를 설정합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-advisors:test
./gradlew :Spring-AI-Agent-Learning:labs:08-advisors:bootRun
curl -sS -c /tmp/learning08.cookies -b /tmp/learning08.cookies \
  http://127.0.0.1:8098/api/advisors/demo \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '예산은 3만원이야.'
curl -sS -c /tmp/learning08.cookies -b /tmp/learning08.cookies \
  http://127.0.0.1:8098/api/advisors/demo \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '내 예산은?'
```

steps는 `A:before → B:before → B:after → A:after`, 두 번째 응답의 storedMessages는 4를 기대합니다.
다음 요청은 도구를 허용합니다. 도구를 지원하는 모델이 필요합니다.

```bash
curl -sS -c /tmp/learning08.cookies -b /tmp/learning08.cookies \
  'http://127.0.0.1:8098/api/advisors/price?tools=true' \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '허브티의 상품 가격을 조회해줘.'
```

도구를 한 번 호출하면 B의 before/after가 두 번, A는 한 번 나타납니다. 실제 모델의 도구 선택 횟수는 달라질 수 있습니다.
실제 호출에는 비용이 발생할 수 있습니다. 종료는 Ctrl+C, DELETE `/api/advisors/demo`는 현재 세션의 해당 대화만 초기화합니다.

## 실험과 테스트

- A/B의 order를 바꾸면 기록 횟수와 순서가 어떻게 바뀌는지 관찰하고 원복합니다.
- `.defaultAdvisors(...)`를 빼면 이전 대화가 자동으로 전달되지 않습니다.
- `memory.add()`를 중복 추가하지 않고 저장 메시지 수를 확인합니다.

[AdvisorContractTest](../../../labs/08-advisors/src/test/java/com/example/ailearning/l08advisors/AdvisorContractTest.java)는 순서, 메모리 재전달/중복 방지, 도구 반복 시 안팎 기록, 세션별 격리·삭제를 로컬 모델 API로 검증합니다.
Advisor가 응답의 사실성을 자동 보장하거나 tool 결과 전체를 장기 저장하는 것은 아닙니다.

## 확인 질문

1. order가 작은 Advisor의 after는 언제 실행되나요?
2. 07의 직접 저장과 메모리 Advisor를 함께 사용하면 왜 문제가 될까요?
3. 도구 결과가 모델 재요청에는 있는데 다음 대화 메모리에는 없을 수 있나요?

<details><summary>해설</summary>

요청에는 먼저, 응답에는 나중에 실행됩니다. 수동 저장과 Advisor 저장을 중복 적용하면 같은 턴이 두 번 들어갈 수 있습니다.
현재 구성은 도구 루프 밖에서 user와 최종 assistant만 저장하므로 가능합니다. 다음 단원에서는 모델 호출 없이 검색용 벡터부터 배웁니다.
</details>

보충: [공식 Advisors API](https://docs.spring.io/spring-ai/reference/api/advisors.html).
[이전: 07](../07-chat-memory.md) · [학습 지도](../../curriculum.md)

[다음: 09. 임베딩과 검색](../09-embeddings-search.md)

## 여기서 멈춰 확인하기

코드를 닫고 “이번 입력 → 새로 하는 처리 → 확인할 결과”를 한 문장으로 적어 보세요.
설명하기 어렵다면 위 핵심 메서드만 다시 읽습니다. 테스트용 HTTP 서버 구현을 이해해야 다음으로 갈 수 있는 것은 아닙니다.

[이전](02-memory-loop.md) · [단원 목차](../08-advisors.md) · [학습 방법](../../learning-guide.md)
