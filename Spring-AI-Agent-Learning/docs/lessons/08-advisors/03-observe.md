# 08-3. 순서와 호출 횟수 실험

**실행 코드·설정·테스트:** [labs/08-00-advisors](../../../labs/08-00-advisors/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** Advisor의 위치에 따라 실행 순서와 반복 횟수가 달라지는 이유를 설명하고, 대화가 중복 저장되지 않는지 확인합니다.

**코드 읽기 안내:** 한 번에 옵션 하나만 바꾸고 실행 기록을 비교합니다. 앞서 배운 메모리 저장 순서가 기준입니다.

## 실행

파일 읽기: TraceAdvisor → AdvisorChatService → [PriceTools](../../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/PriceTools.java) → [AdvisorController](../../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/AdvisorController.java).
PriceTools는 05에서 배운 @Tool 방식으로 가상 허브티 가격 12000원을 조회하는 작은 도구입니다.
명령은 저장소 루트에서 실행합니다. bootRun 전 [01장 루트 .env 설정](../01-setup.md#4-실제-모델-연결을-위한-설정-준비)를 설정합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests com.example.ailearning.l08advisors.AdvisorLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:bootRun
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

### 1. 고정 응답으로 A/B의 위치 비교하기

실제 모델은 도구 선택 횟수가 달라질 수 있으므로, 먼저 로컬 고정 응답 테스트로 Advisor 위치만 비교합니다.

1. 위 로컬 테스트 명령으로 변경 전 결과를 확인합니다. 콘솔 응답의 `steps`와 테스트의 기대 배열을 함께 읽습니다.
2. [AdvisorChatService.chat()](../../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/AdvisorChatService.java)의 A/B order만 서로 바꿉니다. A를 `toolCallingAdvisorOrder + 10`, B를 `toolCallingAdvisorOrder - 10`으로 지정합니다. 질문과 고정 모델 응답은 유지합니다.
3. 아래 두 테스트에서 `steps`의 기대 배열만 표의 변경 후 값으로 바꿉니다. 다른 검증은 유지합니다. 동작을 바꾸는 실험이므로 기존 순서를 검사하는 기대값도 함께 달라집니다.
4. 위 명령으로 로컬 테스트를 다시 실행해 실제 `steps`와 예상한 순서·횟수를 비교합니다. Gradle 테스트는 새 애플리케이션을 시작하므로 `bootRun`은 필요 없습니다. `bootRun`으로 실험한다면 소스 변경 후 서버를 종료하고 다시 실행해야 합니다.
5. Service의 order와 테스트의 기대 배열을 모두 변경 전으로 복원하고 로컬 테스트가 다시 통과하는지 확인합니다.

| 테스트 메서드 | 변경 전 `steps` | 변경 후 `steps` |
|---|---|---|
| `Advisor의_응답_처리는_요청의_반대_순서로_실행된다` | A:before → B:before → B:after → A:after | B:before → A:before → A:after → B:after |
| `도구_루프_안쪽과_바깥쪽_Advisor의_실행_횟수가_다르다` | A:before → B:before → B:after → B:before → B:after → A:after | B:before → A:before → A:after → A:before → A:after → B:after |

도구 없는 요청에서는 두 Advisor가 각각 한 번 실행되고 순서만 바뀝니다. 도구를 한 번 실행하는 고정 시나리오에서는 루프 안쪽으로 옮긴 A가 두 번, 바깥쪽으로 옮긴 B가 한 번 실행됩니다.

### 2. 메모리 Advisor의 역할 확인하기

첫 실험을 복원한 뒤 별도로 수행합니다. `AdvisorChatService` 생성자의 메모리 등록인 `.defaultAdvisors(MessageChatMemoryAdvisor.builder(conversationMemory).build())`만 임시로 제거합니다.
같은 테스트 파일의 `메모리_Advisor는_이전_대화를_중복_저장_없이_전달한다`에서 확인할 변경 후 결과는 `storedMessages=0`, 두 번째 모델 요청에 첫 질문이 없음, 전송 메시지 수 2개(system·현재 user)입니다. 이 세 기대값만 바꾸고 해당 메서드를 선택해 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*AdvisorLocalModelTest.메모리_Advisor는_이전_대화를_중복_저장_없이_전달한다'
```

이 실험은 기존 메모리 동작을 의도적으로 없애므로 다른 메모리 테스트의 기대값과도 달라집니다. 해당 메서드만 비교한 뒤 Service와 테스트의 변경을 모두 복원하고 전체 로컬 테스트를 다시 실행합니다.
현재 구현에서는 `conversationMemory.add()`를 수동으로 추가하지 않습니다. 메모리 Advisor가 저장을 담당하는 원래 구성에서, 같은 대화 두 번 요청 후 `storedMessages=4`인지 확인합니다.

[AdvisorLocalModelTest](../../../labs/08-00-advisors/src/test/java/com/example/ailearning/l08advisors/AdvisorLocalModelTest.java)는 순서, 메모리 재전달/중복 방지, 실패한 호출의 user 메시지 보존, 도구 반복 시 안팎 기록, 세션별 격리·삭제를 로컬 모델 API로 검증합니다.
Advisor가 응답의 사실성을 자동 보장하거나 tool 결과 전체를 장기 저장하는 것은 아닙니다.

## 실무에 연결하기: Advisor에 넣을 처리 고르기

여러 상담 서비스가 같은 방식으로 대화를 전달해야 한다면 메모리 Advisor를 공유할 수 있습니다. 다만 **상품 가격이 예산 이하인지 판단하는 규칙**까지 공통 Advisor에 넣으면 호출 전후 처리와 업무 정책이 섞입니다. 업무 검증은 서비스·도메인에 남겨 책임을 나눕니다.

| 운영 요구 | 확인할 경계 |
|---|---|
| 사용자별 대화 | 대화 ID 전달 전에 서버가 소유권 확인 |
| 실패한 질문 미저장 | 기본 메모리 Advisor의 선저장 정책이 요구와 맞는지 확인 |
| 실행 시간·횟수 수집 | 전체 요청과 도구 루프 안의 모델 요청을 구분 |
| 스트리밍 응답 | `CallAdvisor` 구현만으로 `.stream()`까지 처리한다고 가정하지 않음 |
| 공유 인스턴스 사용 | 이번 요청의 기록 목록을 공유 필드에 저장하지 않음 |

이 예제는 원문 대신 단계 이름을 기록합니다. 원문 로그나 별도 측정 시스템을 추가한다면 필요한 정보와 저장 범위를 먼저 정합니다. 측정 API는 [12-4](../12-evaluation/04-actuator.md), 영구 메모리와 동시 요청은 [07-4](../07-chat-memory/04-production-memory.md)에서 연결합니다.

참고: [Spring AI Advisors API](https://docs.spring.io/spring-ai/reference/api/advisors.html)의 호출·스트리밍 인터페이스와 [Spring Blog의 2.0 도구 루프 설명](https://spring.io/blog/2026/06/15/spring-ai-composable-tool-calling/)을 참고했습니다. 위의 업무 배치는 이 교재의 설계 제안입니다.

## 확인 질문

1. order가 작은 Advisor의 after는 언제 실행되나요?
2. 07의 직접 저장과 메모리 Advisor를 함께 사용하면 왜 문제가 될까요?
3. 도구 결과가 모델 재요청에는 있는데 다음 대화 메모리에는 없을 수 있나요?
4. 현재처럼 A는 도구 루프 밖, B는 안에 두고 도구를 1회 실행한 뒤 최종 답변을 받으면, 왜 A의 before·after는 각 1회이고 B는 각 2회인가요?

<details>
<summary>해설</summary>

1. 요청에서 먼저 들어간 Advisor는 응답에서 나중에 나옵니다. 따라서 order가 작은 Advisor의 after가 더 나중에 실행됩니다.
2. 같은 질문과 답변을 수동 코드와 Advisor가 각각 저장해 대화가 중복될 수 있기 때문입니다.
3. 가능합니다. 현재 메모리 Advisor는 도구 루프 밖에서 user와 최종 assistant만 저장합니다. tool 결과는 같은 요청의 모델 재요청에는 전달되지만 다음 대화의 메모리에는 저장하지 않습니다.
4. A는 전체 도구 루프를 한 번 감쌉니다. B는 최초 질문과 도구 실행 결과를 전달하는 두 모델 요청을 각각 감싸므로 두 번 실행됩니다. 따라서 이 배치와 종료 조건에서는 `steps`에 A의 before·after가 각 1회, B의 before·after가 각 2회 나타납니다.

</details>

보충: [공식 Advisors API](https://docs.spring.io/spring-ai/reference/api/advisors.html).

[이전](02-memory-loop.md) · [다음](../08-advisors.md) · [단원 목차](../08-advisors.md) · [학습 방법](../../learning-guide.md)
