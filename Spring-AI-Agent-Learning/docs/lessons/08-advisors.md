# 08. Advisor: 호출 전후 처리를 조합하기

## 이번 수업의 목표

선수 지식은 07장의 메시지 조회·저장과 05장의 도구 호출입니다.

- Advisor의 요청 전·응답 후 처리와 실행 순서를 설명합니다.
- 메모리의 조회·전달·저장을 Advisor에 연결하고 직접 저장 방식과 비교합니다.
- 도구 루프 안팎에서 기록한 횟수가 달라지는 이유를 실행 기록으로 확인합니다.

## 개념 먼저 이해하기

**Advisor(어드바이서)**는 모델 호출의 앞뒤에 필요한 처리를 연결하는 Spring AI의 기능입니다. 이전 대화를 질문에 추가하거나 호출 기록을 남기는 등, 여러 AI 기능에서 반복되는 처리를 일관되게 적용할 때 사용합니다.

예를 들어 상담 서비스에서는 모델에 질문하기 전에 이전 대화를 함께 준비하고, 응답을 받은 뒤 대화를 저장해야 합니다. Spring AI가 제공하는 메모리 Advisor를 연결하면 이런 처리를 각 질문마다 직접 작성하는 부담을 줄일 수 있습니다.

| 용어 | 쉬운 설명 | 예 |
|---|---|---|
| Advisor | 모델과 주고받는 요청·응답의 처리 과정에 참여하는 기능 | 메모리 연결, 호출 기록 |
| 요청 전 처리 | 모델에 보내기 전에 입력을 준비하거나 보완하는 작업 | 이전 대화 추가 |
| 응답 후 처리 | 받은 응답을 저장하거나 후속 작업에 사용하는 처리 | 모델 답변을 대화 메모리에 저장 |
| Advisor 체인 | 여러 Advisor를 정해진 순서로 연결한 구성 | 대화 준비와 호출 기록을 함께 적용 |

Advisor는 애플리케이션 안에서 실행되는 처리 코드입니다. 이름 때문에 또 다른 AI가 답변에 조언하는 기능으로 생각하기 쉽지만, 별도의 모델이 있어야 하는 것은 아닙니다.

어떤 정보를 모델에 보낼지, 무엇을 언제 저장할지에 따라 서비스의 동작이 달라집니다. 개발팀은 제공되는 Advisor와 직접 구현한 처리를 조합합니다. 이 단원에서는 연결 순서에 따라 실행 기록과 대화 저장이 어떻게 달라지는지 확인합니다.

개념 참고: [Spring AI의 Advisors API](https://docs.spring.io/spring-ai/reference/api/advisors.html).

## 실습 안내

선수 지식: 07의 직접 메시지 조회·저장, 05의 도구 호출.
이번에는 **반복되는 호출 전후 작업을 Advisor로 옮기는 것**을 배웁니다.
[그림 기준](../diagram-guide.md): 시퀀스는 되돌아오는 순서, 플로우차트는 도구 루프와 처리 위치를 보여줍니다.

## Spring AI API를 코드와 연결하기

| API | 역할 | 이 단원에서 읽을 위치 |
|---|---|---|
| `CallAdvisor` | `.call()` 요청·응답을 처리하는 인터페이스 | 08-1의 `TraceAdvisor.adviseCall()` |
| `CallAdvisorChain.nextCall(...)` | 다음 Advisor로 넘기고 응답을 받음 | before와 after 사이 |
| `getOrder()` | 작은 값부터 요청 진입, 응답은 역순 | A·B의 순서 비교 |
| `MessageChatMemoryAdvisor` | 메모리 조회·전달·저장 연결 | 08-2의 기본 Advisor 등록 |
| `ToolCallingAdvisor` | 모델의 도구 요청을 실행하고 결과로 재요청하는 루프 연결 | A·B 배치의 기준 |
| `StreamAdvisor` | `.stream()`의 스트리밍 처리용 인터페이스 | 이번 실습에서는 구현하지 않는 선택 확장 |

공통 기능은 생성 시 `.defaultAdvisors(...)`로 등록하고, 이번 요청의 처리나 매개변수는 `.advisors(...)`로 추가합니다. 대화 ID는 메모리 Advisor가 사용할 키이지 로그인 권한을 검사하는 값은 아닙니다.

### 한 사용자 요청과 여러 모델 요청

사용자가 가격을 물어 모델이 도구를 한 번 사용한 뒤 최종 답변을 한다면, 모델에는 최초 질문과 도구 결과로 총 두 번 요청합니다. 전체 흐름을 감싸는 Advisor는 한 번, 루프 안의 Advisor는 두 번 실행될 수 있습니다. 따라서 기록 횟수를 사용자 질문 수로 바로 해석하지 않습니다.

### 실무에서는 어디까지 맡길까?

공통 메모리 연결·측정처럼 여러 서비스에서 반복하는 처리는 Advisor로 모으고, 상품 예산 검증이나 주문 승인 같은 업무 결정은 서비스·도메인 코드에 둡니다. 요청별 기록 목록은 요청마다 만들고, 공유 Advisor 필드에 여러 사용자의 상태를 섞지 않습니다. 상세 내용과 실패 시 저장 정책은 [08-2](08-advisors/02-memory-loop.md), 운영 관점의 연결은 [08-3](08-advisors/03-observe.md)에 있습니다.


## 작은 수업으로 나누어 읽기

처음에는 아래 순서로 하나씩 학습합니다. 전체 코드를 한 번에 이해하거나 보조 코드를 직접 작성할 필요는 없습니다.

| 순서 | 소수업 | 완료 기준 |
|---|---|---|
| 1 | [호출 전후를 감싸는 Advisor](08-advisors/01-chain.md) | 여러 Advisor의 요청 전·응답 후 처리 순서를 이해합니다. |
| 2 | [메모리와 도구 루프의 위치](08-advisors/02-memory-loop.md) | 앞에서 직접 만든 문맥 전달을 Advisor와 연결합니다. |
| 3 | [순서와 호출 횟수 실험](08-advisors/03-observe.md) | Advisor의 위치에 따라 실행 순서와 반복 횟수가 달라지는 이유를 설명하고, 대화가 중복 저장되지 않는지 확인합니다. |

각 소수업은 이 단원의 실습 모듈을 함께 사용합니다. 표의 순서대로 개념과 실행 코드를 연결하세요.
[공통 학습 방법](../learning-guide.md) · [전체 교육 계획](../curriculum.md)

## 실행 코드와 테스트

[labs/08-00-advisors의 실행 안내](../../labs/08-00-advisors/README.md)를 확인합니다.
이 단원의 기본 구현은 다음 명령으로 테스트·빌드·실행합니다. 명령은 저장소 루트 기준입니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests com.example.ailearning.l08advisors.AdvisorLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:bootRun
```

로컬 테스트는 고정 응답으로 순서·메시지 전달을 검증합니다. 실제 모델은 도구 사용 횟수가 달라질 수 있으며 `bootRun` 후 질문에는 API 비용이 발생할 수 있습니다.

## 확인 질문

1. A의 order가 0, B가 10이고 둘 다 다음 처리를 호출한다면 요청과 정상 응답은 어떤 순서로 지나가나요?
2. `MessageChatMemoryAdvisor`를 등록하고 서비스에서도 같은 메시지를 저장하면 어떤 문제가 생기나요?
3. 도구 루프 안의 기록이 두 번 있다고 사용자도 두 번 질문했다고 볼 수 있나요?

<details>
<summary>해설</summary>

1. 요청은 A → B, 응답은 B → A입니다. 먼저 진입한 A가 B의 반환을 기다렸다가 응답을 처리합니다.
2. 같은 대화가 중복 저장될 수 있습니다. 조회·저장 책임을 한 방식에 두고 실패 시 저장 정책도 확인해야 합니다.
3. 아닙니다. 한 질문을 처리하면서 도구 결과로 모델에 재요청할 수 있습니다. 사용자 요청과 모델 요청의 측정 범위를 구분합니다.

</details>

## 참고 자료

- [Spring AI Advisors API](https://docs.spring.io/spring-ai/reference/api/advisors.html): 인터페이스와 순서의 기준입니다.
- [Christian Tzolov / Spring Blog — Tool Calling in Spring AI 2.0](https://spring.io/blog/2026/06/15/spring-ai-composable-tool-calling/): 도구 루프가 Advisor 체인에 위치하는 설명을 참고했습니다.
- [07~13장 참고 자료 안내](../references.md#0713장-보강에-사용한-교육-자료): 외부 강의·예제의 확인 범위와 버전 주의점을 정리했습니다.

[이전: 07장](07-chat-memory.md) · [다음: 09장](09-embeddings-search.md)
