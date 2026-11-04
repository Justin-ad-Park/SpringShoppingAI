# 학습용 공통 테스트 도구

로컬 HTTP 모델 서버와 결과 출력을 제공합니다. 실제 모델을 호출하거나 답변 내용을 판단하지 않습니다.
각 실습에서는 `testImplementation project(':Spring-AI-Agent-Learning:test-support')`로만 사용합니다.
Spring Boot 애플리케이션이 아니며, 실습의 실행 JAR이나 Live 테스트의 모델 연결에는 포함되지 않습니다.

## 내부 코드의 역할

| 코드 | 책임 |
|---|---|
| `StubHttpServer` | HTTP 요청 수신·기록·응답 전송. 응답 내용은 등록된 함수에 위임 |
| `OpenAiStubServer` | 채팅·임베딩 시나리오 설정과 요청 기록 조회 |
| `ChatResponseSettings` | 채팅 응답·HTTP 상태·지연·사용량 설정 변경과 초기화, 요청별 스냅샷 생성 |
| `SequentialChatResponder` | 정해진 메시지 순서·다음 인덱스 관리, 응답 소진 시 실패 |
| `OpenAiResponseBody` | OpenAI 형식의 JSON 생성 |
| `AnthropicStubServer` | Anthropic 시나리오와 메시지 응답 생성. 현재 한 가지 응답 형식이라 별도 생성 클래스를 두지 않음 |
| `TestOutput` | 테스트 이름과 결과 출력. 기본은 콘솔이며 `PrintStream`을 지정할 수도 있음 |

채팅 설정은 `ChatResponseSettings`가 관리하고, 요청을 처리할 때 변경되지 않는 `Snapshot`으로 한 번 읽습니다.
`OpenAiStubServer`는 설정 변경과 순차 응답 실행을 각각 위 두 클래스에 위임합니다. 실습에서는 서버의 공개 메서드로 응답을 설정합니다.
응답 생성이나 전송 지연 중 설정이 바뀌어도 진행 중인 요청에는 기존 설정을 적용합니다.
토큰 사용량은 배열의 위치 대신 `TokenUsage`의 `input`과 `output`으로 구분합니다.

## 메서드 이름 기준

| 동작 | 이름 예시 |
|---|---|
| 객체·JSON·스냅샷 생성 | `createChatResponseBody`, `createSnapshot` |
| 테스트 응답 설정 | `setChatResponseMessage`, `setChatHttpStatus` |
| 기록·주소 조회 | `getChatRequests`, `getBaseUrl` |
| 콘솔 출력 | `printValue`, `printHttpResponse` |
| 기록 삭제·설정 초기화 | `clearRecordedRequests`, `resetResponseKeepingTokenUsage` |

HTTP 처리 흐름은 `processRequestAndSendResponse` → `readJsonRequest` → `createResponseForRequestPath` → `sendJsonResponse` 순서입니다.
`createResponseForRequestPath`는 경로에 등록된 응답 함수를 실행하고, 등록되지 않은 경로나 응답 생성 실패에는 오류 응답을 만듭니다.
`close`와 `apply`는 각각 `AutoCloseable`과 `Function`의 계약을 따르므로 이름을 유지합니다. record의 자동 접근자도 Java의 기본 형식을 따릅니다.

## 테스트에서 바꾸는 부분

```java
MODEL.setChatResponseMessage(createAssistantTextMessage("고정 응답"));
MODEL.setChatResponseMessage(createAssistantTextMessage("{\"category\":\"음료\",\"maxPrice\":15000}"));
MODEL.setChatResponseSequence(
        createAssistantToolCallMessage("search-1", "searchProducts", "{\"category\":\"음료\"}"),
        createAssistantTextMessage("허브티 12000원입니다."));
```

위는 `OpenAiStubServer MODEL`을 준비한 테스트 안의 사용 예시입니다.
`createAssistantTextMessage`와 `createAssistantToolCallMessage`는 `OpenAiStubServer`의 static 메서드이며, 도구 호출을 모델이 판단하는 예제가 아니라 정해진 HTTP 응답을 만드는 코드입니다.
전체 사용 예시는 [04장 로컬 테스트](../labs/04-structured-output/src/test/java/com/example/ailearning/l04structured/StructuredOutputContractTest.java)를 읽습니다.

| API | 역할 |
|---|---|
| `setChatResponseMessage(createAssistantTextMessage(...))` | 정해진 assistant 응답 지정. JSON 문자열도 content에 담음 |
| `setChatResponseSequence(...)` | 도구 요청·답변 등을 순서대로 반환. 소진되면 HTTP 500 |
| `setChatResponseFunction(request -> ...)` | 이번 요청의 메시지에 따라 응답 분기 |
| `setEmbeddingVectorFunction(text -> float[])` | 테스트용 벡터 지정. 상품·정책별 값은 각 단원에서 정의 |
| `setChatHttpStatus(code)`, `setChatResponseDelay(Duration)`, `setChatTokenUsage(input, output)` | 실패·지연·채팅 사용량 시나리오 설정 |
| `getChatRequests()`, `getEmbeddingRequests()` | 실제로 받은 JSON 요청 조회 |
| `resetChatResponsesAndRequests()` | 채팅 기록·응답·오류·지연 초기화. 임베딩 기록·정의와 usage는 유지 |

`getBaseUrl()`은 OpenAI에는 `/v1`을 포함하고, Anthropic에는 서버 주소만 반환합니다. 제공 업체별 연결 설정에 해당 주소를 그대로 사용합니다.
각 클래스가 자기 서버를 만들고 `@AfterAll`에서 닫습니다. 요청 기록에 인증 헤더를 조회할 수 있지만 콘솔에는 출력하지 않습니다.
임베딩 응답의 usage는 프로토콜 확인용 고정 3토큰이며 실제 입력 길이나 과금 계산이 아닙니다.

## 결과 출력

`new TestOutput(테스트이름)`을 준비하고 `printValue("조건 객체", criteria)` 또는 `printHttpResponse(httpResponse)`로 이름과 실제 결과를 함께 출력합니다.
출력은 assertion을 대신하지 않습니다.

## 도구 자체 검증

저장소 루트에서 외부 API 키 없이 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:test-support:test
```

텍스트·도구 순서·usage·임베딩·오류 기록·지연 중 설정 변경·서버 격리·Anthropic 형식을 확인합니다.
