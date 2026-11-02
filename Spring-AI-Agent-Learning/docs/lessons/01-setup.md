# 01. 환경과 프로젝트 준비

예상 시간: 1시간. 먼저 [00장](00-llm-and-spring-ai.md)을 읽으세요.
이번 목표는 프로젝트를 열고 **실제 API 키 없이 테스트를 통과시키는 것**입니다.
실제 모델 호출은 02장에서 합니다.

## 1. 프로젝트 위치와 Java 확인

macOS/Linux 터미널 기준입니다. SpringShoppingAI 저장소 루트로 이동하세요.

```bash
cd /Users/justinpark/idea/AI/SpringShoppingAI
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

루트 `settings.gradle`은 실습을 등록합니다.

```groovy
rootProject.name = 'spring-shopping-ai'
include 'chatbot-client', 'shopping-agent', 'shopping-mcp'
include 'integration-tests'
include 'Spring-AI-Tutor'
include 'Spring-AI-Agent-Learning:labs:01-first-chat'
include 'Spring-AI-Agent-Learning:labs:04-structured-output'
include 'Spring-AI-Agent-Learning:labs:05-tool-calling'
include 'Spring-AI-Agent-Learning:labs:06-workflow-agent'
include 'Spring-AI-Agent-Learning:labs:07-chat-memory'
```

`:Spring-AI-Agent-Learning:labs:01-first-chat:test`는 교육 폴더의 첫 실습만 테스트한다는 뜻입니다. 다른 서비스를 시작하지 않습니다.
등록과 공통 버전은 저장소가 관리하고, 교육용 코드는 별도 폴더에 둡니다.

`Spring-AI-Agent-Learning/labs/01-first-chat/build.gradle`의 전체 내용입니다.

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
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:test
```

성공 시 마지막에 `BUILD SUCCESSFUL`이 나옵니다.
보고서는 `Spring-AI-Agent-Learning/labs/01-first-chat/build/reports/tests/test/index.html`을 브라우저로 열어 확인합니다.

테스트 경로는 다음과 같습니다.

```text
테스트 HTTP 요청 → 실제 Controller → 실제 ChatService → 실제 Spring AI
  → 로컬 가짜 모델 HTTP 서버 → 고정 응답 반환
```

테스트는 더미 키와 가짜 모델 이름을 자체 설정합니다. 실제 계정이나 `.env`를 읽지 않습니다.
확인하는 내용은 ① 모델에 전달되는 질문·모델 이름·인증 헤더 ② 빈 입력·과도한 입력 거부 ③ 이전 요청이 자동 전달되지 않음입니다.
자연어 이해, 실제 모델 접근 권한, 실제 모델 품질은 확인하지 않습니다.
가짜 서버는 테스트 실행 중에만 동작합니다. `bootRun`을 위한 가짜 모델 실행 모드는 제공하지 않습니다.

## 4. 실제 모델을 사용할 때의 환경 변수

**환경 변수**는 코드 밖에서 프로세스에 전달하는 설정입니다.
모델 API 제공 업체에서 발급한 키와 계정에서 사용 가능한 Chat 모델 이름이 필요합니다.
기존 저장소의 키를 자동으로 복사하거나 읽지 않습니다.

zsh에서 다음 명령을 실행하면 키를 화면과 명령 기록에 직접 쓰지 않고 입력할 수 있습니다.

```zsh
read -rs 'OPENAI_API_KEY?API key: '
echo
export OPENAI_API_KEY
read 'OPENAI_CHAT_MODEL?사용 가능한 Chat 모델 이름: '
export OPENAI_CHAT_MODEL
```

bash라면 다음을 사용하세요.

```bash
read -rs -p 'API key: ' OPENAI_API_KEY
echo
export OPENAI_API_KEY
read -r -p '사용 가능한 Chat 모델 이름: ' OPENAI_CHAT_MODEL
export OPENAI_CHAT_MODEL
```

키 입력 중에는 화면에 문자가 보이지 않습니다. 입력 후 Enter를 누르세요.
모델 이름은 예시 문구를 그대로 쓰지 말고 계정의 실제 모델 이름을 입력합니다.
이 명령을 실행한 **동일 터미널**에서 `bootRun`을 실행해야 합니다.
IDE 실행 버튼은 터미널의 환경 변수를 자동으로 받지 않을 수 있습니다.

`Spring-AI-Agent-Learning/labs/01-first-chat/src/main/resources/application.yml`은 이 값을 읽습니다.

```yaml
server:
  address: 127.0.0.1
  port: ${LEARNING_PORT:8091}

spring:
  application:
    name: first-chat
  ai:
    model:
      embedding: none
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: ${OPENAI_CHAT_MODEL}
```

`${변수}`는 환경 변수 등의 설정에서 값을 찾습니다.
`${LEARNING_PORT:8091}`은 포트 설정이 없으면 8091을 사용한다는 뜻입니다.
`embedding: none`은 아직 사용하지 않는 임베딩 모델 자동 설정을 끕니다. 임베딩은 후속 수업에서 다룹니다.
API 키와 모델 이름에는 기본값을 두지 않아, 잘못된 설정을 조기에 발견하도록 했습니다.

Spring Boot는 `.env` 파일을 놓았다는 이유만으로 자동으로 읽지 않습니다.
이 실습은 위 환경 변수 방식만 사용합니다. 실제 키를 YAML이나 Git에 기록하지 마세요.

## 5. 막혔을 때

| 증상 | 먼저 확인할 것 |
|---|---|
| `Permission denied` | 저장소 루트에서 `chmod +x gradlew` |
| `JAVA_HOME`/Java 버전 오류 | 터미널 `java -version`, IDE의 Gradle JVM, Java 21 설치 |
| 의존성 다운로드 실패 | 인터넷, 프록시, Maven Central·Gradle 배포 서버 접근 |
| 키·모델 placeholder 오류 | 같은 터미널에서 변수를 입력했는지 확인. 값을 출력해 키를 노출하지 않기 |
| 포트 충돌 | 다음 실행에 `LEARNING_PORT=8092 ./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:bootRun` 사용 |

테스트까지는 API 키가 없어도 됩니다. 다음 장의 실제 호출은 키와 모델 접근 권한이 필요합니다.

## 확인 질문

1. Starter와 BOM은 각각 무슨 역할인가요?
2. 로컬 테스트 성공이 실제 모델의 답변 품질을 보장하나요?
3. 터미널에 설정한 키가 새 터미널이나 IDE에서도 반드시 적용되나요?

<details>
<summary>해설</summary>

Starter는 기능 의존성과 자동 설정을 제공하고, BOM은 버전 조합을 관리합니다.
로컬 테스트는 연결 계약을 확인하며 실제 모델의 품질을 보장하지 않습니다.
환경 변수는 프로세스와 자식 프로세스에 전달되므로 다른 실행 환경에서는 별도 설정이 필요합니다.
테스트를 통과시키고 이 세 가지를 설명하면 다음 장으로 넘어가세요.
</details>

보충 읽기: [Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html)의 버전·BOM,
[OpenAI Chat](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html)의 자동 설정 부분.

[이전: 00](00-llm-and-spring-ai.md) · [다음: 02. 첫 모델 호출](02-first-call.md)
