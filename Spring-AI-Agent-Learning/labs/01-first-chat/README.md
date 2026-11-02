# 실습 01: 첫 모델 호출

먼저 [교재 01장](../../docs/lessons/01-setup.md)을 읽습니다.
아래 명령은 모두 **저장소 루트**에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:test
```

실제 모델은 저장소 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`을 자동으로 읽습니다. YAML에 실제 키를 넣지 않습니다.
IDE 실행 시 Working directory를 저장소 루트로 설정합니다. 다른 위치에서는 `LEARNING_ENV_FILE`로 파일의 절대 경로를 지정할 수 있습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:bootRun
```

서버를 시작하지 않고 실제 응답을 출력하는 테스트:

```bash
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:liveOpenAiTest
```

- `FirstChatLocalMockModelTest`: 기본 `test`에서 실행. 로컬 고정 응답, `.env`와 외부 API 사용 없음.
- `FirstChatOpenAiLiveTest`: 위 전용 명령으로 실행. 실제 OpenAI 호출 1회, 질문·응답 출력, API 사용량 발생.

`bootRun`으로 시작했다면 다른 터미널에서:

```bash
curl -i http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '의존성 주입이 무엇인지 쉽게 설명해줘.'
```

종료: 서버 터미널 Ctrl+C.

읽는 순서:

1. `src/main/resources/application.yml`: 환경 변수와 모델 설정
2. `FirstChatApplication.java`: 일반 Boot 시작 코드
3. `ChatController.java`: HTTP 본문을 받고 입력 검증
4. `ChatService.java`: ChatClient로 모델 연결
5. 테스트 (선택): `modelProperties()`와 정상 호출 테스트부터 읽기. 가짜 서버 구현은 제공된 보조 코드로 사용

첫 실습에서는 설정과 `ChatService.ask()`의 호출 흐름을 이해하면 충분합니다.
입력 검증 테스트는 보충, 대화 기억 테스트는 03장, 인증 헤더 검증은 선택 심화로 읽습니다.
테스트 설정과 두 HTTP 서버의 역할은 [02장의 테스트 코드 읽기](../../docs/lessons/02-first-call.md#테스트-코드-읽기-선택)에서 설명합니다.

이 실습은 이전 대화·도구·RAG를 사용하지 않습니다.
테스트의 고정 응답은 실제 AI 답변이 아닙니다. 실제 계정 호출은 `bootRun` 또는 `liveOpenAiTest`로 수행합니다.
학습용 외부 오류는 기본 500으로 나타날 수 있으며 서버 로그에서 원인을 확인합니다.

[전체 코드와 실행 해설](../../docs/lessons/02-first-call.md) · [프롬프트 변경 실험](../../docs/lessons/03-chatclient-and-prompts.md)
