# 실습 01: 첫 모델 호출

먼저 [교재 01장](../../docs/lessons/01-setup.md)을 읽습니다.
아래 명령은 모두 **저장소 루트**에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test --tests com.example.ailearning.FirstChatLocalMockModelTest
```

실제 모델은 저장소 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`을 자동으로 읽습니다. YAML에 실제 키를 넣지 않습니다.
Gradle의 `bootRun`과 `test`는 작업 디렉터리를 저장소 루트로 고정합니다. IDE에서 Gradle을 거치지 않고 직접 실행할 때도 Working directory를 저장소 루트로 설정합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:bootRun
```

테스트는 연결 대상에 따라 두 클래스로 나뉩니다.

- `FirstChatLocalMockModelTest`: 로컬 MockServer의 고정 응답을 사용합니다. 실제 키가 필요 없습니다.
- `FirstChatOpenAiLiveTest`: 루트 `.env`로 실제 OpenAI에 접속하고 응답을 출력합니다.

IntelliJ에서는 원하는 클래스 또는 메서드의 Run을 누르면 됩니다. 별도 실행 허용 변수나 태그 설정은 없습니다.
터미널에서 실제 테스트만 실행하려면:

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test --tests com.example.ailearning.FirstChatOpenAiLiveTest --rerun-tasks
```

필터 없이 이 모듈의 `test`를 실행하면 두 클래스가 모두 실행됩니다.
실제 테스트는 API 사용량이 발생합니다. `--rerun-tasks`는 이전 결과를 재사용하지 않고 다시 실행하려는 옵션입니다.

`bootRun`으로 시작했다면 다른 터미널에서:

```bash
curl -i http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '의존성 주입이 무엇인지 쉽게 설명해줘.'
```

종료: 서버 터미널 Ctrl+C.

읽는 순서:

1. `src/main/resources/application.yml`: 루트 `.env` 읽기와 모델 설정
2. `FirstChatApplication.java`: 일반 Boot 시작 코드
3. `ChatController.java`: HTTP 본문을 받고 입력 검증
4. `ChatService.java`: ChatClient로 모델 연결
5. 테스트 (선택): `modelProperties()`와 정상 호출 테스트부터 읽기. 가짜 서버 구현은 제공된 보조 코드로 사용

첫 실습에서는 설정과 `ChatService.ask()`의 호출 흐름을 이해하면 충분합니다.
입력 검증 테스트는 보충, 대화 기억 테스트는 03장, 인증 헤더 검증은 선택 심화로 읽습니다.
테스트 설정과 두 HTTP 서버의 역할은 [02장의 테스트 코드 읽기](../../docs/lessons/02-first-call/02-test-code.md)에서 설명합니다.

이 실습은 이전 대화·도구·RAG를 사용하지 않습니다.
테스트의 고정 응답은 실제 AI 답변이 아닙니다. 실제 계정 호출은 `bootRun` 또는 루트 `.env`를 읽는 Live 테스트로 수행합니다.
학습용 외부 오류는 기본 500으로 나타날 수 있으며 서버 로그에서 원인을 확인합니다.

[전체 코드와 실행 해설](../../docs/lessons/02-first-call.md) · [프롬프트 변경 실험](../../docs/lessons/03-chatclient-and-prompts.md)

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다. 이 의존성은 테스트에만 적용됩니다. 각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `FirstChatLocalMockModelTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `FirstChatOpenAiLiveTest`: 실제 OpenAI에 연결합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 `[한글 테스트 메서드명]`과 응답·검증 대상 값을 함께 확인합니다. 출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test --tests com.example.ailearning.FirstChatLocalMockModelTest
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test --tests com.example.ailearning.FirstChatOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다. 클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.
