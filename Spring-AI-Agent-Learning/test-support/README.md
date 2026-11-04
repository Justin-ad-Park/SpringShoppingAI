# 학습용 공통 테스트 도구

로컬 HTTP 모델 서버와 결과 출력을 제공합니다. 실제 모델을 호출하거나 답변 내용을 판단하지 않습니다.
각 실습에서는 `testImplementation project(':Spring-AI-Agent-Learning:test-support')`로만 사용합니다.
Spring Boot 애플리케이션이 아니며, 실습의 실행 JAR이나 Live 테스트의 모델 연결에는 포함되지 않습니다.

## 테스트에서 바꾸는 부분

```java
MODEL.respondWith(text("고정 응답"));
MODEL.respondWith(text("{\"category\":\"음료\",\"maxPrice\":15000}"));
MODEL.respondInOrder(
        toolCall("search-1", "searchProducts", "{\"category\":\"음료\"}"),
        text("허브티 12000원입니다."));
```

위는 `OpenAiStubServer MODEL`을 준비한 테스트 안의 사용 예시입니다.
`text`와 `toolCall`은 `OpenAiStubServer`의 static 메서드이며, 도구 호출을 모델이 판단하는 예제가 아니라 정해진 HTTP 응답을 만드는 코드입니다.
전체 사용 예시는 [04장 로컬 테스트](../labs/04-structured-output/src/test/java/com/example/ailearning/l04structured/StructuredOutputContractTest.java)를 읽습니다.

| API | 역할 |
|---|---|
| `respondWith(text(...))` | 정해진 assistant 응답 지정. JSON 문자열도 content에 담음 |
| `respondInOrder(...)` | 도구 요청·답변 등을 순서대로 반환. 소진되면 HTTP 500 |
| `respondUsing(request -> ...)` | 이번 요청의 메시지에 따라 응답 분기 |
| `embeddingUsing(text -> float[])` | 테스트용 벡터 지정. 상품·정책별 값은 각 단원에서 정의 |
| `status(code)`, `delay(Duration)`, `usage(input, output)` | 실패·지연·채팅 사용량 시나리오 설정 |
| `chatRequests()`, `embeddingRequests()` | 실제로 받은 JSON 요청 조회 |
| `resetChat()` | 채팅 기록·응답·오류·지연 초기화. 임베딩 기록·정의와 usage는 유지 |

`baseUrl()`은 OpenAI에는 `/v1`을 포함하고, Anthropic에는 서버 주소만 반환합니다. 제공 업체별 연결 설정에 해당 주소를 그대로 사용합니다.
각 클래스가 자기 서버를 만들고 `@AfterAll`에서 닫습니다. 요청 기록에 인증 헤더를 조회할 수 있지만 콘솔에는 출력하지 않습니다.
임베딩 응답의 usage는 프로토콜 확인용 고정 3토큰이며 실제 입력 길이나 과금 계산이 아닙니다.

## 결과 출력

`new TestOutput(테스트이름)`을 준비하고 `value("조건 객체", criteria)` 또는 `response(httpResponse)`로 이름과 실제 결과를 함께 출력합니다.
출력은 assertion을 대신하지 않습니다.

## 도구 자체 검증

저장소 루트에서 외부 API 키 없이 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:test-support:test
```

텍스트·도구 순서·usage·임베딩·오류 기록·지연 중 설정 변경·서버 격리·Anthropic 형식을 확인합니다.
