# 08. Advisor: AI 호출에 필요한 공통 기능 연결하기

## 이번 수업의 목표

02장의 ChatClient 호출을 이해하면 시작할 수 있습니다.<br>
메모리 예제에서는 07장, 도구 예제에서는 05장의 내용을 연결합니다.

- Advisor가 필요한 이유와 Spring AI가 제공하는 활용을 설명합니다.
- 모든 요청에 적용할 기능과 이번 요청에만 추가할 기능을 구분합니다.
- 메모리 연결을 통해 Advisor 객체와 요청별 설정값의 차이를 익힙니다.
- 기본 사용법을 익힌 뒤 직접 구현과 실행 순서, 도구 루프의 기록 범위를 확인합니다.

## 어떤 문제를 해결할까?

쇼핑 상담에는 질문에 답하는 것 외에도 반복해서 필요한 작업이 있습니다.<br>
모델에 무엇을 보냈는지 확인하고, 앞서 말한 예산을 후속 질문에 포함하고, 관련 배송 정책을 찾아 답변의 근거로 전달해야 합니다.<br>
이를 각 서비스의 모델 호출 앞뒤에 매번 직접 쓰면 같은 기능을 여러 곳에서 관리하게 됩니다.

**Advisor(어드바이저)는 ChatClient의 요청 처리 과정에 연결하는 기능 객체**입니다.<br>
예를 들어 메모리 Advisor를 연결하면 이전 대화를 모델 입력에 준비하고 현재 대화를 저장하는 처리를 맡길 수 있습니다.<br>
Advisor는 애플리케이션에서 실행되는 코드이며, 그 이름이 별도의 AI 모델을 뜻하지는 않습니다.

## 먼저 알아둘 용어

먼저 **Advisor가 하는 일**을 이해하고, 그 일을 코드에서 어떤 인터페이스와 메서드로 표현하는지 연결해 봅니다.

### Advisor의 역할을 설명하는 개념

| 개념 | 쉬운 설명 | 연결되는 코드·예 |
|---|---|---|
| 요청 전 처리 | 모델에 요청을 보내기 전에 하는 작업 | 로거 Advisor가 질문을 기록하거나, 메모리 Advisor가 이전 대화를 입력에 추가 |
| 응답 후 처리 | 모델의 응답을 받은 뒤 하는 작업 | 로거 Advisor가 답변을 기록하거나, 메모리 Advisor가 답변을 대화 기록에 저장 |
| Spring AI가 제공하는 Advisor | 위와 같은 기능을 Spring AI 개발팀이 구현해 라이브러리로 제공한 클래스 | `SimpleLoggerAdvisor`: 요청·응답 기록, `MessageChatMemoryAdvisor`: 대화 메모리 연결 |
| 개발자가 직접 구현하는 Advisor | 서비스에 필요한 처리를 개발팀이 작성한 클래스 | 08-4의 `BeforeAfterAdvisor`: 전후 처리 단계 기록 |
| Advisor 체인 | 여러 Advisor를 연결해 요청과 응답을 처리하는 구성 | 메모리 Advisor와 로거 Advisor를 같은 ChatClient에 연결 |

### 요청 전 처리와 응답 후 처리는 어떻게 이어질까?

동기 호출에서 Advisor는 다음 처리를 호출하기 전에 요청을 준비하고, 응답이 돌아오면 남은 코드를 실행합니다.<br>
`chain.nextCall(request)`가 다음 처리를 호출하는 코드입니다.<br>
그 앞에 요청 전 처리를, 정상 반환 뒤에 응답 후 처리를 작성합니다.<br>
이 메서드 호출 관계를 “Advisor가 다음 처리를 감싼다”고 표현합니다.

수업에서는 아래 세 가지를 한꺼번에 비교하지 않고 차례로 확인합니다.

| 먼저 이해할 것 | 확인할 수업 |
|---|---|
| Advisor 하나가 다음 처리를 기다렸다가 실행을 이어감 | [08-4 요청 전·응답 후 처리](08-advisors/04-chain.md) |
| 두 Advisor의 order가 진입 순서를 정하고, 응답은 반대로 반환됨 | [08-5 두 Advisor의 순서](08-advisors/05-advisor-order.md) |
| 도구 결과를 전달하려고 모델에 다시 요청함 | [08-6 도구 실행과 모델 재요청](08-advisors/06-tool-loop.md) |
| 반복 담당 객체 앞의 Advisor와 뒤의 Advisor는 실행 범위가 다름 | [08-7 반복 처리와 배치](08-advisors/07-tool-loop-order.md) |

### 위 개념을 코드로 표현하는 인터페이스와 메서드

인터페이스는 여러 클래스가 따르는 공통 규칙입니다.<br>
Spring AI가 제공하는 Advisor와 개발자가 직접 구현하는 Advisor 모두 아래 인터페이스를 통해 ChatClient에 연결됩니다.

| 구분 | 실제 API 이름 | 위 개념과의 연결 | 샘플 위치와 확인할 코드 |
|---|---|---|---|
| 인터페이스 | `Advisor` | Advisor의 이름과 실행 순서를 나타내는 공통 규칙. 기능을 제공하는 주체가 Spring AI인지 개발자인지와 무관하게 적용 | [08-4](08-advisors/04-chain.md)의 [BeforeAfterAdvisor](../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/lesson04_beforeafter/BeforeAfterAdvisor.java): `CallAdvisor`를 통해 상속한 `getName()`·`getOrder()` 구현 |
| 인터페이스 | `CallAdvisor` | 일반 응답 방식에서 요청 전·응답 후 처리를 구현하는 규칙. `adviseCall(...)` 안에서 다음 Advisor로 요청을 전달하고 반환된 응답을 처리 | [08-4](08-advisors/04-chain.md)의 [BeforeAfterAdvisor](../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/lesson04_beforeafter/BeforeAfterAdvisor.java): `adviseCall()`의 기록 처리와 `chain.nextCall(request)` |
| 인터페이스 | `StreamAdvisor` | 답변을 조금씩 받는 스트리밍 방식의 처리 규칙. `adviseStream(...)`으로 요청과 응답 흐름에 참여 | 이 과정에는 직접 구현하는 스트리밍 Advisor 샘플 없음 |
| 메서드 | `ChatClient.Builder.defaultAdvisors(...)` | 선택한 Advisor를 이 빌더로 생성하는 **ChatClient의 모든 요청에 적용하도록 등록** | [08-1](08-advisors/01-built-in.md)의 [Lesson01LoggingChatService](../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/lesson01_logging/Lesson01LoggingChatService.java): 생성자에서 로거 Advisor 등록 |
| 메서드 | `ChatClient.ChatClientRequestSpec.advisors(Advisor...)` | 선택한 Advisor를 **지금 구성하는 요청에만 추가하도록 등록** | [08-2](08-advisors/02-registration.md)의 [Lesson02AdvisorRegistrationService](../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/lesson02_registration/Lesson02AdvisorRegistrationService.java): `chatWithAdditionalAdvisor()`에서 추가 Advisor 전달 |
| 메서드 | `ChatClient.AdvisorSpec.param(...)` | 등록한 Advisor가 **이번 요청에서 읽을 설정값을 지정**. 예: 메모리 Advisor가 사용할 대화 ID | [08-3](08-advisors/03-memory.md)의 [Lesson03MemoryAdvisorChatService](../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/lesson03_memory/Lesson03MemoryAdvisorChatService.java): `chat()`에서 `param(ChatMemory.CONVERSATION_ID, conversationKey)` 설정 |

예를 들어 `SimpleLoggerAdvisor`는 Spring AI가 제공하는 클래스이고, 일반 응답을 처리할 때 `CallAdvisor`의 규칙을 따릅니다.<br>
이 객체를 `defaultAdvisors(...)`에 전달하면 해당 ChatClient가 질문을 처리할 때마다 요청·응답을 기록합니다.<br>
즉, **Advisor 구현 클래스는 어떤 기능을 사용할지**, **등록 메서드(`defaultAdvisors` 또는 `advisors`)는 그 기능을 어느 요청에 적용할지**를 정합니다.

### ‘Spring AI가 제공한다’와 ‘모든 요청에 적용한다’는 서로 다른 설명이다

‘기본 제공 Advisor’라는 표현을 보더라도 **Spring AI가 구현해서 제공하는 Advisor**라는 뜻으로 읽으세요.<br>
반면 `defaultAdvisors(...)`의 `default`는 **그 ChatClient가 모든 요청에 사용할 설정**이라는 뜻입니다.<br>
두 뜻이 섞이지 않도록 이 문서에서는 ‘기본’만으로 줄여 부르지 않습니다.

예를 들어 Spring AI가 제공하는 `SimpleLoggerAdvisor`도 이번 요청에만 추가할 수 있고,
개발자가 직접 만든 `BeforeAfterAdvisor`도 ChatClient의 모든 요청에 적용할 수 있습니다.<br>
Advisor를 누가 구현했는지와 어느 요청에 적용할지는 따로 선택합니다.

## defaultAdvisors와 advisors를 한 문장으로 구분하면

**`defaultAdvisors(...)`는 등록한 Advisor를 그 ChatClient의 모든 요청에 적용하고, `.prompt()` 뒤의 `advisors(Advisor...)`는 지정한 Advisor를 이번 요청에만 추가합니다.**

예를 들어 `defaultAdvisors(...)`로 로거 Advisor를 등록하고, 이번 요청의 `advisors(Advisor...)`에 진단용 Advisor를 추가했다고 합시다.<br>
이번 요청에는 로거와 진단용 Advisor가 함께 적용됩니다.<br>
다음 질문을 새 `.prompt()`로 구성하면서 진단용 Advisor를 추가하지 않으면 로거만 적용됩니다.<br>
이번 요청에 Advisor를 추가해도 모든 요청에 적용하도록 등록한 로거가 사라지는 것은 아닙니다.

`advisors(spec -> spec.param(...))`는 Advisor 객체 대신 **설정용 함수**를 받는 형태입니다.<br>
이 코드의 `param(...)`은 이미 등록한 Advisor가 사용할 값을 지정하며 새 Advisor를 추가하지 않습니다.<br>
구체적인 사용 코드는 [08-2](08-advisors/02-registration.md)·[08-3](08-advisors/03-memory.md)에서, 여러 Advisor를 함께 적용할 때의 실행 순서는 [08-5](08-advisors/05-advisor-order.md)에서 배웁니다.

## 어떤 기능이 제공될까?

이 과정이 사용하는 **Spring AI 2.0.2의 배포 소스**를 기준으로 정리했습니다.<br>
기능을 선택해 등록하는 클래스와 프레임워크 내부에서 사용하는 클래스를 구분합니다.<br>
클래스 이름을 누르면 해당 버전의 공식 소스를 볼 수 있습니다.<br>
‘샘플 위치’가 빈 항목은 이 과정에 실행 샘플이 없습니다.

### Spring AI 2.0.2에서 기본 제공되는 Advisor

| 용도 | 클래스 | 하는 일 | 샘플 위치 |
|---|---|---|---|
| 로깅 | [`SimpleLoggerAdvisor`](https://github.com/spring-projects/spring-ai/blob/v2.0.2/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/SimpleLoggerAdvisor.java) | 요청과 정상 응답을 DEBUG 로그로 출력 | [08-1](08-advisors/01-built-in.md) |
| 대화 메모리 | [`MessageChatMemoryAdvisor`](https://github.com/spring-projects/spring-ai/blob/v2.0.2/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/MessageChatMemoryAdvisor.java) | 이전 대화를 메시지 목록으로 모델 입력에 넣고 현재 대화를 저장 | [08-3](08-advisors/03-memory.md) |
| 대화 메모리 | [`VectorStoreChatMemoryAdvisor`](https://github.com/spring-projects/spring-ai/blob/v2.0.2/advisors/spring-ai-vector-store-advisor/src/main/java/org/springframework/ai/chat/client/advisor/vectorstore/VectorStoreChatMemoryAdvisor.java) | 벡터 저장소에서 관련 대화를 검색해 시스템 메시지에 넣고 대화를 저장 |  |
| 검색 기반 답변(RAG) | [`QuestionAnswerAdvisor`](https://github.com/spring-projects/spring-ai/blob/v2.0.2/advisors/spring-ai-vector-store-advisor/src/main/java/org/springframework/ai/chat/client/advisor/vectorstore/QuestionAnswerAdvisor.java) | 벡터 저장소에서 질문과 관련된 문서를 검색해 모델 입력에 추가 | [10-2](10-rag/02-standard-advisor.md) |
| 검색 기반 답변(RAG) | [`RetrievalAugmentationAdvisor`](https://github.com/spring-projects/spring-ai/blob/v2.0.2/spring-ai-rag/src/main/java/org/springframework/ai/rag/advisor/RetrievalAugmentationAdvisor.java) | 질문 변환·문서 검색·문맥 구성 등 여러 검색 단계를 조합해 답변 생성을 지원 |  |
| 도구 호출 | [`ToolCallingAdvisor`](https://github.com/spring-projects/spring-ai/blob/v2.0.2/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/ToolCallingAdvisor.java) | 모델이 요청한 도구를 실행하고 결과를 모델에 다시 보내는 반복 호출을 처리 | [08-6](08-advisors/06-tool-loop.md) |
| 도구 탐색 | [`ToolSearchToolCallingAdvisor`](https://github.com/spring-projects/spring-ai/blob/v2.0.2/advisors/spring-ai-tool-search-advisor/src/main/java/org/springframework/ai/chat/client/advisor/toolsearch/ToolSearchToolCallingAdvisor.java) | 많은 도구 중 필요한 도구를 검색하고 그 정의를 모델에 순차적으로 제공하며 실행 |  |
| 입력 차단 | [`SafeGuardAdvisor`](https://github.com/spring-projects/spring-ai/blob/v2.0.2/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/SafeGuardAdvisor.java) | 사용자 입력에 설정한 금칙어가 있으면 모델 호출을 차단하고 정해진 응답을 반환. 단순 단어 검사이므로 종합적인 유해성 판정 기능은 아님 |  |
| 구조화 응답 검증 | [`StructuredOutputValidationAdvisor`](https://github.com/spring-projects/spring-ai/blob/v2.0.2/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/StructuredOutputValidationAdvisor.java) | 모델의 JSON 응답을 스키마로 검사하고 실패하면 오류를 알려 정해진 횟수 안에서 재요청. 스트리밍 응답은 지원하지 않음 |  |
| 응답 캐시 | [`SemanticCacheAdvisor`](https://github.com/spring-projects/spring-ai/blob/v2.0.2/vector-stores/spring-ai-redis-semantic-cache/src/main/java/org/springframework/ai/chat/cache/semantic/SemanticCacheAdvisor.java) | 의미가 비슷한 질문의 캐시된 답변을 찾으면 재사용하고, 없으면 모델을 호출해 답변을 캐시에 저장 |  |

Spring AI가 제공한다는 것은 라이브러리에 해당 기능을 구현한 클래스가 있다는 뜻입니다.<br>
모두가 08장 모듈에 포함되거나 자동으로 활성화되는 것은 아닙니다.<br>
벡터 검색·RAG·도구 탐색·캐시는 해당 모듈 의존성과 저장소 등 필요한 설정을 추가해야 사용할 수 있습니다.<br>
`ToolCallingAdvisor`는 이 과정의 Spring AI 2.0.2 기본 구성에서 ChatClient가 자동 연결합니다.<br>
08-6에서는 도구 반복 자체를, 08-7에서는 그 앞뒤에 배치한 Advisor의 실행 범위를 비교합니다.

### ChatClient가 자동으로 연결하는 모델 호출용 Advisor

Spring AI의 `ChatClient`는 내부 구현인 `DefaultChatClient`에서 Advisor 체인의 마지막에 모델 호출용 Advisor를 자동으로 연결합니다.<br>
일반 응답 요청은 `ChatModelCallAdvisor`가, 스트리밍 응답 요청은 `ChatModelStreamAdvisor`가 실제 `ChatModel`에 전달합니다.<br>
개발자는 `.call()` 또는 `.stream()`으로 호출 방식을 선택합니다.

| 클래스 | 하는 일 | 샘플 위치 |
|---|---|---|
| [`ChatModelCallAdvisor`](https://github.com/spring-projects/spring-ai/blob/v2.0.2/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/ChatModelCallAdvisor.java) | 일반 호출을 `ChatModel.call(...)`에 전달 | |
| [`ChatModelStreamAdvisor`](https://github.com/spring-projects/spring-ai/blob/v2.0.2/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/ChatModelStreamAdvisor.java) | 스트리밍 호출을 `ChatModel.stream(...)`에 전달 | |

Spring AI가 제공하는 Advisor로 해결되지 않는 공통 처리는 [08-4](08-advisors/04-chain.md)처럼 직접 구현할 수도 있습니다.

## 학습 순서와 샘플의 연결

한 실행 모듈에서 **소수업별 Service와 테스트를 분리**합니다.<br>
수업 번호가 클래스 이름에도 들어 있어 문서와 코드를 함께 찾을 수 있습니다.<br>
처음에는 Spring AI가 제공하는 로거 Advisor 하나를 사용하고, 그 다음 적용할 요청의 범위·설정값을 익힌 뒤 내부 실행 구조로 넘어갑니다.

| 순서 | 학습할 내용 | 전용 Service | 확인할 결과 |
|---|---|---|---|
| [08-1. Spring AI가 제공하는 로거 사용](08-advisors/01-built-in.md) | 제공 기능을 연결하고 사용하기 | `Lesson01LoggingChatService` | 요청·응답 로그 |
| [08-2. 등록 범위](08-advisors/02-registration.md) | 모든 요청에 적용하는 등록과 이번 요청에만 추가하는 등록 | `Lesson02AdvisorRegistrationService` | 로거는 모든 요청에 적용하고 추가 로거는 이번 요청에만 적용 |
| [08-3. 메모리와 파라미터](08-advisors/03-memory.md) | 메모리 Advisor와 대화 ID 설정 람다 | `Lesson03MemoryAdvisorChatService` | 후속 요청에 이전 대화 포함 |
| [08-4. Advisor 하나의 전후 처리](08-advisors/04-chain.md) | nextCall의 정상 반환 전후 | `Lesson04BeforeAfterChatService` | 요청 전 → 모델 → 응답 후 |
| [08-5. 두 Advisor의 순서](08-advisors/05-advisor-order.md) | order와 반대 방향 반환 | `Lesson05AdvisorOrderChatService` | A 전 → B 전 → B 후 → A 후 |
| [08-6. 도구 실행과 모델 재요청](08-advisors/06-tool-loop.md) | 한 질문에 여러 모델 요청이 필요한 이유 | `Lesson06ToolLoopChatService` | 도구 결과를 담은 두 번째 모델 요청 |
| [08-7. 도구 반복과 Advisor 배치](08-advisors/07-tool-loop-order.md) | DEFAULT_ORDER를 기준으로 배치 | `Lesson07ToolLoopOrderChatService` | 전체 처리 1회와 각 모델 요청의 기록 |

## 실행하기

모듈은 [labs/08-00-advisors](../../labs/08-00-advisors/README.md) 하나입니다.<br>
설정·실행 경로와 테스트 목록은 README에 있습니다.<br>
저장소 루트에서 다음 명령으로 각 소수업의 로컬 테스트를 실행합니다.<br>
외부 API 호출은 없습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*LocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:bootJar
```

각 소수업의 Live 테스트는 루트 `.env`로 실제 OpenAI를 호출하며 API 사용량이 발생합니다.<br>
로컬 테스트는 고정 응답과의 연결·설정 동작을 확인합니다.<br>
실제 모델의 문장이나 도구 선택 횟수가 같음을 보장하지는 않습니다.

## 확인 질문

1. 여러 상담 서비스가 같은 호출 기록 기능을 사용하려면 Advisor를 어떻게 활용할 수 있나요?
2. 특정 질문에만 필요한 Advisor를 `defaultAdvisors(...)`로 등록하면 어떤 요청에 적용되나요?
3. 대화 ID를 요청마다 바꾸려면 메모리 Advisor를 매번 새로 구현해야 하나요?

<details>
<summary>해설</summary>

1. Spring AI가 제공하는 `SimpleLoggerAdvisor` 또는 개발자가 기록 기능을 직접 구현한 Advisor를 각 ChatClient에 연결해 재사용할 수 있습니다.
2. 해당 ChatClient의 모든 요청에 적용됩니다.<br>
   특정 질문을 처리할 때만 필요하다면 그 요청을 구성하는 `.prompt().advisors(Advisor...)`에 추가합니다.
3. 아닙니다.<br>
   `defaultAdvisors(...)`로 메모리 Advisor를 모든 요청에 적용하도록 등록하고, 각 요청에서는 `advisors(spec -> spec.param(...))`으로 사용할 대화 ID를 지정할 수 있습니다.

</details>

## 참고 자료

Advisor 목록은 [Spring AI 2.0.2 공식 소스](https://github.com/spring-projects/spring-ai/tree/v2.0.2)를 기준으로 작성했습니다.

- [Spring AI Advisors API](https://docs.spring.io/spring-ai/reference/api/advisors.html): Advisor의 역할·인터페이스·순서 기준.
- [Spring AI ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html): ChatClient의 모든 요청에 적용하는 설정·현재 요청의 설정·로깅 사용법.
- [Spring AI Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html): 메모리 연결과 대화 ID.
- [Spring Blog — Tool Calling in Spring AI 2.0](https://spring.io/blog/2026/06/15/spring-ai-composable-tool-calling/): Advisor 체인의 도구 루프.
- [07~13장 참고 자료 안내](../references.md#0713장-보강에-사용한-교육-자료): 외부 자료의 확인 범위와 버전 주의점.

[학습 방법](../learning-guide.md) · [교육 계획](../curriculum.md) · [이전: 07장](07-chat-memory.md) · [시작: 08-1](08-advisors/01-built-in.md) · [다음 단원: 09장](09-embeddings-search.md)
