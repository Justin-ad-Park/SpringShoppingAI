# 02-2. 테스트 코드 읽기 (선택)

첫 실습의 필수 학습 범위는 `application.yml`의 연결 설정과 세 클래스의 역할,
`ChatService.ask()`의 네 줄입니다. 테스트는 제공된 검증 도구로 실행하면 충분하며,
가짜 HTTP 서버를 직접 구현하는 것은 이번 학습 목표에 포함하지 않습니다.

테스트도 읽고 싶다면 [FirstChatLocalModelTest.java](../../../labs/02-00-first-chat/src/test/java/com/example/ailearning/FirstChatLocalModelTest.java)에서 다음 순서로 확인하세요.

1. `modelProperties()`: 테스트용 연결 설정
2. `로컬_모델에_질문을_보내고_고정_응답을_받는다()`: 질문이 전달되고 응답 문자열이 반환되는지 확인
3. `잘못된_입력은_로컬_모델_호출_전에_거부한다()`: 입력 검증 보충. Spring MVC/Validation의 동작 확인

`잘못된_입력은_로컬_모델_호출_전에_거부한다()`는 `"가".repeat(ChatController.MAX_MESSAGE_LENGTH + 1)`로
허용 길이보다 정확히 한 글자 긴 입력을 만듭니다. Controller와 같은 상수를 사용하며, HTTP 400과 모델 요청 0회를 확인합니다.

`다음_요청에_이전_대화를_자동으로_포함하지_않는다()`는 03장의 대화 기억 실험에서 읽습니다.
인증 헤더 검증은 선택 심화입니다. `ask()`는 애플리케이션 요청을 보내며, 가짜 서버와 출력은 [공통 테스트 도구](../../../test-support/README.md)가 제공합니다.

로컬 MockModel 테스트는 `spring.config.import=`로 루트 `.env` 읽기를 끕니다.
`modelProperties()`의 설정은 `@DynamicPropertySource`를 통해 **테스트에서만** 적용됩니다.
`application.yml`의 같은 설정을 덮어쓰거나 테스트에 필요한 설정을 추가합니다.

| 설정 | 실제 실행 (`bootRun`) | 로컬 테스트 |
|---|---|---|
| `spring.ai.openai.api-key` | 루트 `.env`의 `OPENAI_API_KEY` | 더미 키 `local-test-key` |
| `spring.ai.openai.chat.options.model` | 루트 `.env`의 `OPENAI_MODEL` | 검증용 이름 `tutor-test-model` |
| `spring.ai.openai.base-url` | `application.yml`의 OpenAI 기본 주소 | 테스트가 시작한 로컬 가짜 서버 주소 |

더미 키도 Spring AI의 연결 설정에 사용하므로 유지합니다. 가짜 서버는 이 키로 실제 계정 인증을 하지 않습니다.
테스트의 인증 헤더 검증은 `Bearer local-test-key`가 요청에 담겼는지 확인합니다.

테스트 중에는 두 개의 HTTP 서버가 동작합니다.

```text
테스트의 ask() → 애플리케이션 서버의 /api/chat → Controller → ChatService → Spring AI
  → 가짜 모델 서버의 /v1/chat/completions → 고정 JSON 응답 → 응답 텍스트 반환
```

`RANDOM_PORT`는 애플리케이션 서버의 포트를 배정합니다. 공통 `OpenAiStubServer`도 별도 사용 가능한 포트로 시작하며, `getBaseUrl()`로 주소를 제공합니다. 테스트는 애플리케이션의 포트를 `@Value`로 받습니다.
가짜 서버의 고정 JSON은 Spring AI가 읽을 수 있는 OpenAI 응답 형식을 흉내 낸 것이며 AI가 생성한 답변은 아닙니다.

<details><summary>선택 참고: 가짜 서버의 종료와 요청 기록 공유</summary>

`resetRequests()`는 테스트마다 `MODEL_SERVER.chat().resetResponsesAndRequests()`로 기록과 이전 시나리오를 초기화한 뒤 고정 응답을 지정합니다.
`@AfterAll`의 `close()`는 서버와 실행 작업을 정리합니다. HTTP 처리와 요청 기록은 공통 모듈이 담당하므로 첫 실습에서는 직접 작성하지 않아도 됩니다.

</details>

## Mock 테스트 실행

저장소 루트에서 아래 명령으로 해당 클래스만 실행할 수 있습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-00-first-chat:test --tests com.example.ailearning.FirstChatLocalModelTest
```

[이전: 생성자 주입](01-constructor-injection.md) · [다음: 실제 모델 호출](03-live-model-call.md) · [02장 본문](../02-first-call.md)
