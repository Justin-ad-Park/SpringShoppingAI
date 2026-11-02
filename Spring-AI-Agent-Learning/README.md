# Spring AI Agent Learning

Spring Boot로 간단한 API를 만들 수 있지만 AI와 Spring AI는 처음인 개발자를 위한 한국어 교재입니다.
목표는 **한 번의 모델 호출을 이해하는 것부터 시작해, 도구를 선택하는 작은 Agent로 확장하는 것**입니다.

이 폴더는 SpringShoppingAI 저장소의 교육용 하위 프로젝트입니다. 기초 실습은 다른 서비스를 실행하지 않고 사용할 수 있으며, 이후 MCP 수업에서 shopping-mcp와 연결합니다. 기존 Spring-AI-Tutor 교재와는 별도 폴더로 관리합니다.

## 이번에 작성한 범위

2026-10-29 첫 묶음: 기초 교재 00~03, 실행 가능한 첫 호출 실습 1개, 전체 교육 계획과 레퍼런스 검토.
구조화 응답 이후의 교재·코드는 다음 묶음으로 계획했으며 아직 구현하지 않았습니다.
특히 Workflow/Agent 비교는 [교육 계획의 상세 설계](docs/curriculum.md#workflow와-agent-비교-수업)로 먼저 정의했습니다.

## 어디부터 읽을까요?

| 순서 | 문서 | 이번 단계에서 할 일 |
|---|---|---|
| 1 | [00. LLM과 Spring AI](docs/lessons/00-llm-and-spring-ai.md) | 용어와 전체 호출 흐름 이해 |
| 2 | [01. 환경과 프로젝트](docs/lessons/01-setup.md) | Java·Gradle·의존성·설정 확인, API 키 없이 테스트 |
| 3 | [02. 첫 모델 호출](docs/lessons/02-first-call.md) | 세 클래스의 전체 코드 읽기, 실제 모델 호출 |
| 4 | [03. ChatClient와 프롬프트](docs/lessons/03-chatclient-and-prompts.md) | 호출 코드 해설, system/user 메시지와 대화 기억 실험 |
| 이후 | [전체 교육 계획](docs/curriculum.md) | 이후 배울 개념과 완료 기준 확인 |
| 필요할 때 | [레퍼런스 안내](docs/references.md) | 지금 읽을 공식 문서 부분만 선택 |

처음부터 전체 공식 문서를 읽지 않아도 됩니다. 00 → 01 → 02 → 03 순으로 읽으세요.
뒤쪽의 확인 질문에는 문서 안에 접을 수 있는 해설이 있습니다.

## 실습 환경

- Java 21, Gradle Wrapper 8.14.5
- Spring Boot 4.0.9, Spring AI 2.0.2
- 첫 실습: OpenAI Chat 어댑터, 로컬 주소 `127.0.0.1:8091`
- IDE: 상위 SpringShoppingAI 저장소를 Gradle 프로젝트로 열기
- DB, Docker, 프런트엔드, 기존 쇼핑 애플리케이션 불필요

버전은 저장소 루트 build.gradle과 Gradle Wrapper에서 모든 실습·서비스에 공통으로 관리합니다.
모델 이름은 계정에서 사용할 수 있는 것으로 직접 설정합니다. 특정 모델이나 가격을 교재에 고정하지 않습니다.

## 빠른 확인

상위 SpringShoppingAI 저장소 루트에서 실행합니다. 첫 실행은 인터넷으로 Gradle과 라이브러리를 받습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:test
```

위 명령은 외부 모델을 호출하지 않습니다. 실제 Spring AI가 **로컬 가짜 모델 HTTP 서버**에 보낸 요청을 검증합니다.
자연어 이해 성능이나 실제 모델의 정답률을 평가하는 테스트는 아닙니다.

실제 모델 호출은 [환경 설정](docs/lessons/01-setup.md#4-실제-모델을-사용할-때의-환경-변수) 후 다음 명령으로 시작합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:bootRun
```

다른 터미널에서:

```bash
curl -i http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw 'Spring의 의존성 주입을 초보자에게 두 문장으로 설명해줘.'
```

실제 모델 호출에는 API 사용량과 비용이 발생할 수 있습니다. 시작/종료와 오류 해결은 01·02장을 참고하세요.

## 폴더 안내

```text
docs/
  curriculum.md          전체 순서와 다음 제작 범위
  references.md          자료별 장점·주의점·읽을 부분
  verification.md        실제 검증 범위와 결과
  lessons/00~03          이번에 완성한 한국어 교재
labs/
  01-first-chat/         다른 서비스 없이 실행 가능한 Spring Boot 실습
../gradlew               저장소 공통 Gradle Wrapper
```

학습 코드의 클래스 읽기 순서는 `FirstChatApplication` → `ChatController` → `ChatService`입니다.
테스트 내부의 가짜 서버 구현은 첫 실습의 선수 지식이 아닙니다.
