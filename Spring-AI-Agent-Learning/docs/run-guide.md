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
| `02-00-first-chat`, `02-05-model-timeout-retry`, `04`~`08`(05 보충 모듈 포함), `11-00-mcp-workflow-agent`, `12-02-model-evaluation`, `12-04-observability` | `OPENAI_API_KEY`, `OPENAI_MODEL` |
| `09-00-embeddings-search` | `OPENAI_API_KEY`, `OPENAI_EMBEDDING_MODEL` — 채팅 모델은 사용하지 않음 |
| `10-00-rag`, `10-04-rag-advisor`, `12-00-evaluation`, `13-00-shopping-agent` | `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_EMBEDDING_MODEL` |
| `02-04-multiple-models` | `OPENAI_API_KEY`, `OPENAI_MODEL`, `ANTHROPIC_API_KEY`, `ANTHROPIC_MODEL` |

두 제공 업체를 사용하는 선택 실습에서만 Anthropic 키와 모델 ID를 추가합니다.
모델 ID는 계정에서 접근 가능하고 해당 실습의 기능을 지원하는 값을 사용합니다.

모델 서버의 기본 주소는 각 모듈의 `application.yml`에 둡니다.

```yaml
spring:
  ai:
    openai:
      base-url: https://api.openai.com/v1
```

Live 테스트는 이 주소를 그대로 사용합니다. 로컬 고정 응답 테스트만 `@DynamicPropertySource`에서
`spring.ai.openai.base-url`을 테스트 서버 주소로 덮어씁니다. 테스트 서버의 포트는 실행할 때 정해지므로 Java에서 등록합니다.
다중 제공 업체 모듈의 Anthropic 기본 주소도 YAML에 두고, 해당 로컬 테스트에서만 덮어씁니다.

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
./gradlew :Spring-AI-Agent-Learning:labs:05-00-tool-calling:bootRun
```

## 3. 테스트 결과를 읽기

클래스 이름으로 테스트의 범위를 구분합니다.

| 이름 | 검증 대상 | 실제 외부 모델 호출 |
|---|---|---|
| `객체명UnitTest` | 객체 하나의 기능. Spring·HTTP 서버 없이 실행 | 없음 |
| `주제LocalModelTest` | 로컬 고정 응답 HTTP 서버를 사용한 모델·도구·웹 연결 | 없음 |
| `주제OpenAiLiveTest` / `주제ProvidersLiveTest` | 실제 OpenAI 또는 여러 제공 업체와의 연결 | 있음 |

`LocalModelTest`는 로컬 LLM 실행을 뜻하지 않습니다. 외부 API 대신 고정 응답 서버를 사용하는 통합 테스트입니다. 객체별 단위 테스트를 제공하는 모듈에서는 README의 로컬 명령이 단위·통합 클래스를 함께 선택합니다.

로컬 Mock 테스트는 `.env` 읽기를 끄고 더미 키·모델 이름·로컬 서버 주소를 직접 설정합니다.
서버의 HTTP 처리와 결과 출력은 [공통 테스트 도구](../test-support/README.md)가 제공하며, 단원별 응답과 시나리오는 각 테스트에 남아 있습니다.
고정 HTTP 응답으로 코드의 연결을 검증하며, 실제 모델의 답변 품질을 평가하는 테스트와 다릅니다.
각 모듈에는 로컬 테스트와 별도 OpenAI Live 테스트가 있습니다. Live 클래스는 모듈 README에서 확인합니다.
테스트 시작 시 빈 줄과 `===== [ 테스트 메서드명 ] ======`을 한 번 출력하고, 그 아래에 HTTP 상태·응답·조건 객체·검색 결과를 이름 반복 없이 출력합니다. 출력 형식은 [공통 테스트 도구](../test-support/README.md#실행과-결과-출력)에서 관리합니다.
Live는 루트 `.env`의 실제 설정을 사용하며, 모듈의 전체 `test`에도 포함됩니다. 로컬 검증은 반드시 아래처럼 클래스를 지정합니다.
태그·실행 허용 변수·별도 Live 태스크는 없습니다. IntelliJ에서도 원하는 클래스나 메서드의 Run을 선택합니다.

첫 실습에서 Mock 테스트만 실행하려면 클래스를 지정하세요.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-00-first-chat:test --tests com.example.ailearning.FirstChatLocalModelTest
```

다중 모델 실습에서도 로컬 테스트만 실행하려면 클래스를 지정합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-04-multiple-models:test --tests com.example.ailearning.l02multiple.MultipleProvidersLocalModelTest
```

[학습 방법](learning-guide.md) · [교육 계획](curriculum.md)

구조화 응답의 실제 OpenAI 연결을 확인하려면:

```bash
./gradlew :Spring-AI-Agent-Learning:labs:04-00-structured-output:test --tests com.example.ailearning.l04structured.StructuredOutputOpenAiLiveTest
```

Live의 문장은 실행마다 달라질 수 있습니다. 실제 도구 선택·검색 품질·평가 결과는 출력으로 확인하며, 고정 응답 테스트의 결과와 구분합니다.

## 11장 MCP 실습

[실행 안내](../labs/11-00-mcp-workflow-agent/README.md)를 따릅니다. 수동 실행은 shopping-mcp(기본 8082)와 학습 앱(기본 8106)을 각각 시작합니다.
로컬 테스트는 MCP 서버를 자동 실행하고 OpenAI 응답만 고정합니다. 실제 외부 모델 테스트는 별도 OpenAiLiveTest 클래스입니다.

## 02·05장의 별도 보충 실습

기본 Workflow 모듈과 각각 독립 실행합니다. 두 모듈 모두 루트 `.env`의 채팅 키·모델만 사용합니다.

| 문서 | 모듈 | 포트 |
|---|---|---|
| [05 보충 5: 도구 호출 횟수 제한](lessons/05-tool-calling/05-tool-call-limits.md) | [05-05-tool-call-limits](../labs/05-05-tool-call-limits/README.md) | 8110 |
| [02 보충 5: 타임아웃·재시도](lessons/02-first-call/05-model-timeout-retry.md) | [02-05-model-timeout-retry](../labs/02-05-model-timeout-retry/README.md) | 8111 |

12장의 실패·관측 수업에서도 같은 로컬 테스트를 실행합니다. 요청 횟수는 고정 HTTP 서버가 받은 기록으로 확인하며, 실제 모델 연결은 각 모듈의 Live 클래스로 구분합니다.
