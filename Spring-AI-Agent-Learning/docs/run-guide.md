# 실습 실행 위치와 모델 설정

모든 Gradle 명령은 SpringShoppingAI 저장소 루트에서 실행합니다.
각 소수업 상단의 모듈 README에 test·bootJar·bootRun 명령이 있습니다.
모든 학습 모듈은 실제 실행 시 **저장소 루트 `.env`를 properties 설정 파일로 읽습니다.**
터미널에서 키를 입력하거나 `export`할 필요가 없으며, zsh·Bash 중 특정 셸을 사용할 필요도 없습니다.

## 1. 루트 .env에 필요한 값 준비하기

```properties
OPENAI_API_KEY=<본인의 OpenAI API 키>
OPENAI_MODEL=<사용 가능한 채팅 모델 ID>
OPENAI_EMBEDDING_MODEL=<사용 가능한 임베딩 모델 ID>
```

위 대괄호 부분은 실제 값으로 바꿉니다. 임베딩 모델은 이를 사용하는 실습에서만 필요합니다.
파일은 `KEY=value` 형식으로 작성하고, `export` 접두어나 값을 감싸는 따옴표는 넣지 않습니다.
키가 담긴 `.env`는 Git에 커밋하지 않습니다. 저장소의 `.gitignore`가 루트 `.env`를 제외합니다.

| 실행 모듈 | 루트 .env에서 사용하는 값 |
|---|---|
| `02-first-chat`, `04`~`08`, `11-model-evaluation`, `11-observability` | `OPENAI_API_KEY`, `OPENAI_MODEL` |
| `09-embeddings-search` | `OPENAI_API_KEY`, `OPENAI_EMBEDDING_MODEL` — 채팅 모델은 사용하지 않음 |
| `10-rag`, `10-rag-advisor`, `11-evaluation`, `12-shopping-agent` | `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_EMBEDDING_MODEL` |
| `02-multiple-models` | `OPENAI_API_KEY`, `OPENAI_MODEL`, `ANTHROPIC_API_KEY`, `ANTHROPIC_MODEL` |

두 제공 업체를 사용하는 선택 실습에서만 Anthropic 키와 모델 ID를 추가합니다.
모델 ID는 계정에서 접근 가능하고 해당 실습의 기능을 지원하는 값을 사용합니다.

## 2. 파일을 읽는 위치와 실행 방법

각 실습의 `application.yml`에 다음 설정이 있습니다.

```yaml
spring:
  config:
    import: "optional:file:.env[.properties]"
```

`.env`는 프로세스 환경 변수를 설정하는 스크립트가 아니라 Spring Boot가 읽는 설정 파일입니다.
`[.properties]`는 파일을 Java properties 형식으로 읽으라는 뜻입니다.
`optional:`은 파일이 없을 때 파일 읽기 오류를 내지 않는다는 뜻이며, 실제 호출에 필요한 설정값까지 생략할 수 있다는 뜻은 아닙니다.

각 모듈의 Gradle `bootRun`과 `test`는 작업 디렉터리를 저장소 루트로 맞춥니다.
IDE에서 Gradle을 거치지 않고 직접 실행한다면 Working directory도 저장소 루트로 맞춥니다.
프로세스 환경 변수에 같은 이름이 이미 설정되어 있다면 파일 값보다 우선하므로, 예상과 다른 모델이 선택될 때 확인하세요.
키나 전체 `.env` 내용을 콘솔에 출력하지 않습니다.

예를 들어 Tool Calling 실습은 루트 `.env`를 준비한 뒤 다음 명령으로 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:05-tool-calling:bootRun
```

## 3. 테스트 결과를 읽기

로컬 Mock 테스트는 `.env` 읽기를 끄고 더미 키·모델 이름·로컬 서버 주소를 직접 설정합니다.
고정 HTTP 응답으로 코드의 연결을 검증하며, 실제 모델의 답변 품질을 평가하는 테스트와 다릅니다.
첫 실습의 Live 클래스만 실제 OpenAI를 호출하며, 해당 모듈의 전체 test에도 포함됩니다.
다른 모듈의 자동 테스트에는 실제 키가 필요 없습니다. 실제 모델 호출은 각 모듈의 bootRun으로 확인합니다.

첫 실습에서 Mock 테스트만 실행하려면 클래스를 지정하세요.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test --tests com.example.ailearning.FirstChatLocalMockModelTest
```

[학습 방법](learning-guide.md) · [교육 계획](curriculum.md)
