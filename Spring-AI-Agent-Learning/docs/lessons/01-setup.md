# 01. 환경과 프로젝트 준비

## 한 번에 읽을 범위

아래 세 주제를 순서대로 학습합니다.
테스트용 HTTP 서버를 직접 구현할 필요는 없습니다.

| 순서 | 읽고 해볼 것 | 여기서 확인할 것 |
|---|---|---|
| 1 | Java·Gradle·의존성 위치 | 실행 명령의 저장소 위치와 첫 모듈 경로 확인 |
| 2 | Mock 테스트만 실행 | 실제 키 없이 연결 계약이 확인됨을 설명 |
| 3 | 루트 .env로 실제 모델 연결 준비 | 키·모델 이름·실행 작업 디렉터리의 관계 설명 |

[공통 학습 방법](../learning-guide.md)

먼저 [00장](00-llm-and-spring-ai.md)을 읽으세요.
이번 목표는 프로젝트를 열고 **실제 API 키 없이 테스트를 통과시키는 것**입니다.
실제 모델 호출은 02장에서 합니다.

## 1. 프로젝트 위치와 Java 확인

macOS/Linux 터미널 기준입니다. SpringShoppingAI 저장소를 내려받은 폴더로 이동하세요.
아래 `/path/to/SpringShoppingAI`는 예시 경로이므로 본인의 저장 위치로 바꿉니다.

```bash
cd /path/to/SpringShoppingAI
java -version
./gradlew --version
```

Java 21, Gradle 8.14.5를 확인합니다. 첫 Wrapper 실행은 인터넷 연결이 필요합니다.
IDE에서는 SpringShoppingAI의 `settings.gradle`을 열고 Gradle JVM을 Java 21로 지정하세요.
교육 자료 하위 폴더에는 별도 Wrapper나 settings.gradle을 두지 않습니다.

**Gradle Wrapper**는 프로젝트가 정한 Gradle 버전을 내려받고 실행하는 작은 실행기입니다.
`gradlew`와 `gradle/wrapper/`가 함께 필요합니다. Gradle을 별도로 설치할 필요는 없습니다.
현재 제공하는 실행 안내는 macOS/Linux 기준이며 Windows에서는 WSL을 사용할 수 있습니다.

## 2. 빌드 파일은 무엇을 설정할까?

루트 [settings.gradle](../../../settings.gradle)은 실습을 등록합니다. 처음에는 첫 실습의 등록 한 줄만 읽습니다. 아래 코드는 파일 전체가 아닌 발췌입니다.

```groovy
include 'Spring-AI-Agent-Learning:labs:02-00-first-chat'
```

`:Spring-AI-Agent-Learning:labs:02-00-first-chat:test`는 교육 폴더의 첫 실습만 테스트한다는 뜻입니다. 다른 서비스를 시작하지 않습니다.
등록과 공통 버전은 저장소가 관리하고, 교육용 코드는 별도 폴더에 둡니다.

`Spring-AI-Agent-Learning/labs/02-00-first-chat/build.gradle`의 의존성 부분입니다.
파일 아래의 작업 디렉터리·테스트 태스크 설정은 제공된 실행 보조 코드로 사용하면 됩니다.

```groovy
// Java·Spring Boot·Spring AI 버전과 공통 의존성은 저장소 루트에서 관리합니다.
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springframework.ai:spring-ai-starter-model-openai'
}
```

**Starter**는 특정 기능에 필요한 라이브러리와 자동 설정을 묶어 제공합니다.
webmvc는 HTTP API, model-openai는 OpenAI 모델 연결을 위한 것입니다. 입력 검증과 테스트 의존성은 루트에서 공통 적용합니다.
**BOM**은 관련 라이브러리들의 버전을 맞추는 목록입니다. 저장소 루트 `build.gradle`의 `spring-ai-bom:2.0.2`가 이를 지정합니다.
같은 파일에서 Boot 4.0.9 플러그인, Java 21, 공통 테스트 의존성을 관리하므로 실습마다 버전을 중복 선언하지 않습니다.
Java 코드가 없는 중간 폴더에는 Boot 플러그인을 적용하지 않습니다.

이번 묶음에서는 Chat 기능만 사용합니다. 다른 모델 기능을 지금 배울 필요는 없습니다.

## 3. API 키 없이 테스트 실행

저장소 루트에서:

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-00-first-chat:test --tests com.example.ailearning.FirstChatLocalModelTest
```

성공 시 마지막에 `BUILD SUCCESSFUL`이 나옵니다.
보고서는 `Spring-AI-Agent-Learning/labs/02-00-first-chat/build/reports/tests/test/index.html`을 브라우저로 열어 확인합니다.

테스트 경로는 다음과 같습니다.

```text
테스트 HTTP 요청 → 실제 Controller → 실제 ChatService → 실제 Spring AI
  → 로컬 가짜 모델 HTTP 서버 → 고정 응답 반환
```

이 Mock 테스트는 더미 키와 가짜 모델 이름을 자체 설정합니다. 실제 계정이나 `.env`를 읽지 않습니다.
`FirstChatLocalModelTest.modelProperties()`의 `@DynamicPropertySource`가 테스트 실행에만 설정을 등록합니다.
`application.yml`의 같은 설정보다 우선 적용되므로 환경 변수에 실제 키와 모델 이름을 넣지 않아도 됩니다.
`base-url`도 로컬 가짜 모델 서버 주소로 지정해 외부 제공 업체로 요청하지 않습니다.
확인하는 내용은 ① 모델에 전달되는 질문·모델 이름·인증 헤더 ② 빈 입력·과도한 입력 거부 ③ 이전 요청이 자동 전달되지 않음입니다.
자연어 이해, 실제 모델 접근 권한, 실제 모델 품질은 확인하지 않습니다.
가짜 서버는 테스트 실행 중에만 동작합니다. `bootRun`을 위한 가짜 모델 실행 모드는 제공하지 않습니다.
이번 장에서는 테스트를 실행하고 결과를 확인하면 충분합니다. 코드 읽기 범위는 [02장](02-first-call/02-test-code.md)을 참고하세요.

## 4. 실제 모델 연결을 위한 설정 준비

**환경 변수**와 **설정 파일**은 값을 전달하는 서로 다른 방법입니다.

| 구분 | 값을 준비하는 방법 | 이 실습에서 읽는 방식 |
|---|---|---|
| 프로세스 환경 변수 | 터미널의 `export`나 IDE의 Environment variables로 실행할 프로세스에 전달 | Spring Boot가 프로세스 환경에서 읽음 |
| 루트 `.env` 설정 파일 | 저장소 루트 파일에 `KEY=value`로 기록 | `spring.config.import`로 properties 설정 파일로 읽음 |

이 실습은 두 번째 방식으로 `OPENAI_API_KEY`, `OPENAI_MODEL`을 준비합니다.
`.env`를 만든다고 운영체제의 환경 변수가 설정되는 것은 아닙니다.
YAML이나 테스트 코드에 실제 키를 적거나, 매번 터미널에서 export할 필요가 없습니다.
현재는 설정만 준비하며, 실제 모델 호출은 02장에서 진행합니다.

설정 파일에는 다음 이름이 있어야 합니다. 아래 값은 설명용이며 실제 키·접근 가능한 모델 이름으로 설정합니다.

```properties
OPENAI_API_KEY=본인의_API_키
OPENAI_MODEL=사용_가능한_Chat_모델_이름
```

현재 실습의 설정 파일 이름은 `application.yml`입니다. `.yaml`도 YAML 확장자이지만 이 저장소에서는 `.yml`을 사용합니다.
Spring Boot는 `.env`라는 이름을 자동 인식하지 않으므로 `spring.config.import`로 명시합니다.
`[.properties]`는 확장자가 없는 설정 파일을 Java properties 형식으로 읽으라는 뜻입니다.
`KEY=value` 형식을 사용하고, 셸의 `export` 접두어나 값을 감싸는 따옴표는 넣지 않습니다.
`.env`를 셸 스크립트로 실행하지 않습니다. 저장소의 `.gitignore`가 루트 `.env`를 제외합니다.

`application.yml` 전체는 다음과 같습니다.

```yaml
server:
  address: 127.0.0.1
  port: ${LEARNING_PORT:8091}

spring:
  config:
    import: "optional:file:.env[.properties]"
  application:
    name: first-chat
  ai:
    model:
      embedding: none
    openai:
      base-url: https://api.openai.com/v1
      api-key: ${OPENAI_API_KEY}
      chat:
        max-retries: 0
        timeout: 30s
        options:
          model: ${OPENAI_MODEL}
```

- `bootRun`과 테스트는 모듈의 `build.gradle`에서 작업 디렉터리를 저장소 루트로 맞춥니다.
- 기본 `.env` 경로는 그 작업 디렉터리 기준입니다. IDE에서 직접 실행할 때도 Working directory를 저장소 루트로 설정합니다.
- `optional:`은 `.env` 파일이 없어도 파일 읽기 오류를 내지 않는다는 뜻입니다. 실제 호출에 필요한 키·모델 값까지 생략할 수 있다는 뜻은 아닙니다.
- 이미 설정한 프로세스 환경 변수는 파일보다 우선합니다. 예상과 다른 모델이면 기존 `OPENAI_MODEL` 설정도 확인합니다.
- `${LEARNING_PORT:8091}`은 포트 설정이 없으면 8091을 사용한다는 뜻입니다.
- `embedding: none`은 아직 배우지 않은 임베딩 자동 설정을 끕니다.
- `max-retries: 0`은 자동 재시도를 끄고, `timeout: 30s`는 SDK 요청 시간 제한입니다.

기본 로컬 MockModel 테스트는 `spring.config.import=`로 파일 읽기를 끄고 더미 설정을 주입합니다.
실제 파일과 외부 API가 없어도 실행할 수 있습니다.

### 설정 준비 후 어디에서 실행할까?

이 장에서는 3절의 Mock 테스트까지 통과하면 됩니다.
실제 API 호출은 [02-3. 실제 모델 호출](02-first-call/03-live-model-call.md)에서 서버 실행과 Live 테스트 중 원하는 경로로 진행합니다.
`FirstChatLocalModelTest`는 로컬 가짜 모델에, `FirstChatOpenAiLiveTest`는 실제 OpenAI에 연결합니다.
두 클래스에는 태그나 별도 실행 조건이 없습니다. **필터 없이 이 모듈의 `test`를 실행하면 둘 다 실행되므로 실제 API 사용량이 발생할 수 있습니다.**

후속 학습 모듈도 루트 `.env`와 `OPENAI_MODEL`을 사용합니다.
임베딩이 필요한 실습은 `OPENAI_EMBEDDING_MODEL`을 추가로 사용합니다. 모듈별 필수 값은 [실행 설정 안내](../run-guide.md)를 확인하세요.

## 5. 막혔을 때

| 증상 | 먼저 확인할 것 |
|---|---|
| `Permission denied` | 저장소 루트에서 `chmod +x gradlew` |
| `JAVA_HOME`/Java 버전 오류 | 터미널 `java -version`, IDE의 Gradle JVM, Java 21 설치 |
| 의존성 다운로드 실패 | 인터넷, 프록시, Maven Central·Gradle 배포 서버 접근 |
| 키·모델 placeholder 오류 | 루트 `.env`의 변수 이름과 IDE 작업 디렉터리 확인. 키 값은 출력하지 않기 |
| 포트 충돌 | 다음 실행에 `LEARNING_PORT=8092 ./gradlew :Spring-AI-Agent-Learning:labs:02-00-first-chat:bootRun` 사용 |

기본 로컬 MockModel 테스트까지는 API 키가 없어도 됩니다. 다음 장의 실제 호출은 키와 모델 접근 권한이 필요합니다.

## 확인 질문

1. Starter와 BOM은 각각 무슨 역할인가요?
2. 로컬 테스트 성공이 실제 모델의 답변 품질을 보장하나요?
3. 터미널에 설정한 키가 새 터미널이나 IDE에서도 반드시 적용되나요?

<details>
<summary>해설</summary>

1. Starter는 기능 의존성과 자동 설정을 제공하고, BOM은 여러 의존성의 버전 조합을 관리합니다.
2. 아닙니다. 로컬 고정 응답 테스트는 연결과 처리 규칙을 확인하며 실제 모델의 답변 품질을 보장하지 않습니다.
3. 아닙니다. 환경 변수는 해당 프로세스와 자식 프로세스에 전달되므로 새 터미널이나 IDE에는 별도 설정이 필요할 수 있습니다.

</details>

테스트를 통과시키고 이 세 가지를 설명하면 다음 장으로 넘어가세요.

보충 읽기: [Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html)의 버전·BOM,
[OpenAI Chat](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html)의 자동 설정 부분.

[이전: 00](00-llm-and-spring-ai.md) · [다음: 02. 첫 모델 호출](02-first-call.md)
