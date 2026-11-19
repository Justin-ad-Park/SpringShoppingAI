# 08 Advisor 실습

[단원 교재](../../docs/lessons/08-advisors.md)의 다섯 소수업을 한 앱에서 실행합니다.<br>
로거 사용 → 등록 범위 → 메모리 파라미터 → 직접 구현·순서 → 도구 루프 순서로 학습합니다.<br>
각 Service는 해당 수업에 필요한 기능만 연결합니다.<br>
`AdvisorController`는 HTTP 입력·세션·공백 검증을 처리하는 공통 진입점입니다.

## 소수업과 실행 코드

| 수업 | 학습할 내용 | Service | LocalModel | OpenAI Live |
|---|---|---|---|---|
| 08-1 | 기본 로거 연결 | [Lesson01LoggingChatService](src/main/java/com/example/ailearning/l08advisors/logging/Lesson01LoggingChatService.java) | [Lesson01LoggingLocalModelTest](src/test/java/com/example/ailearning/l08advisors/logging/Lesson01LoggingLocalModelTest.java) | [Lesson01LoggingOpenAiLiveTest](src/test/java/com/example/ailearning/l08advisors/logging/Lesson01LoggingOpenAiLiveTest.java) |
| 08-2 | 기본·요청별 등록 범위 | [Lesson02AdvisorRegistrationService](src/main/java/com/example/ailearning/l08advisors/registration/Lesson02AdvisorRegistrationService.java) | [Lesson02AdvisorRegistrationLocalModelTest](src/test/java/com/example/ailearning/l08advisors/registration/Lesson02AdvisorRegistrationLocalModelTest.java) | [Lesson02AdvisorRegistrationOpenAiLiveTest](src/test/java/com/example/ailearning/l08advisors/registration/Lesson02AdvisorRegistrationOpenAiLiveTest.java) |
| 08-3 | 대화 메모리와 설정 파라미터 | [Lesson03MemoryAdvisorChatService](src/main/java/com/example/ailearning/l08advisors/memory/Lesson03MemoryAdvisorChatService.java) | [Lesson03MemoryAdvisorLocalModelTest](src/test/java/com/example/ailearning/l08advisors/memory/Lesson03MemoryAdvisorLocalModelTest.java) | [Lesson03MemoryAdvisorOpenAiLiveTest](src/test/java/com/example/ailearning/l08advisors/memory/Lesson03MemoryAdvisorOpenAiLiveTest.java) |
| 08-4 | 직접 구현과 네 Advisor의 순서 | [Lesson04AdvisorChainChatService](src/main/java/com/example/ailearning/l08advisors/chain/Lesson04AdvisorChainChatService.java) | [Lesson04AdvisorChainLocalModelTest](src/test/java/com/example/ailearning/l08advisors/chain/Lesson04AdvisorChainLocalModelTest.java) | [Lesson04AdvisorChainOpenAiLiveTest](src/test/java/com/example/ailearning/l08advisors/chain/Lesson04AdvisorChainOpenAiLiveTest.java) |
| 08-5 | 도구 루프 안팎의 실행 범위 | [Lesson05ToolLoopChatService](src/main/java/com/example/ailearning/l08advisors/toolloop/Lesson05ToolLoopChatService.java) | [Lesson05ToolLoopAdvisorLocalModelTest](src/test/java/com/example/ailearning/l08advisors/toolloop/Lesson05ToolLoopAdvisorLocalModelTest.java) | [Lesson05ToolLoopAdvisorOpenAiLiveTest](src/test/java/com/example/ailearning/l08advisors/toolloop/Lesson05ToolLoopAdvisorOpenAiLiveTest.java) |

08-2는 Service를 직접 주입받아 Java Advisor 객체를 등록하는 예제이며 전용 HTTP API는 만들지 않습니다.<br>
08-4의 `TraceAdvisor`와 `chain/support/AdvisorTrace`는 08-5의 기록에도 재사용합니다.

## 로컬 테스트와 빌드

저장소 루트에서 실행합니다.<br>
고정 응답 HTTP 모델 서버로 Spring AI의 실제 연결 동작을 검사하며 외부 유료 API를 호출하지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*LocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:bootJar
# 예: 등록 범위 수업만 실행
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*Lesson02AdvisorRegistrationLocalModelTest'
```

테스트 고정 설정은 `src/test/resources/application-test.yml`, 실행 시 결정되는 대역 URL은 `DynamicPropertySource`에 있습니다.<br>
통신·모델 대역·결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다.<br>
IntelliJ에서도 위 클래스나 한글 메서드의 Run을 선택할 수 있습니다.

## 실제 모델과 앱 실행

루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`이 필요하며, 08-5에는 도구 호출을 지원하는 모델을 지정합니다.<br>
아래 Live 테스트와 앱 실행 후 질문에는 실제 API 사용량이 발생합니다.<br>
클래스 필터 없는 `test`에도 Live가 포함됩니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*OpenAiLiveTest'
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:bootRun
```

| 수업 | API | 본문·결과 |
|---|---|---|
| 08-1 | `POST /api/advisors/logging` | 질문 텍스트 → 답변 텍스트, 콘솔 요청·응답 로그 |
| 08-3 | `POST /api/advisors/memory/{conversationId}` | 예산·후속 질문 → `answer`, `storedMessages` |
| 08-3 | `DELETE /api/advisors/memory/{conversationId}` | 현재 세션의 대화 삭제 |
| 08-4 | `POST /api/advisors/chain` | 질문 텍스트 → `answer`, `steps` |
| 08-5 | `POST /api/advisors/tool-loop` | 허브티 가격 질문 → `answer`, `steps` |

본문은 `text/plain; charset=UTF-8`, 포트는 8098입니다.<br>
curl과 결과 해석은 각 소수업에 있습니다.<br>
메모리 대화를 이어가려면 같은 쿠키를 사용합니다.<br>
서버 종료는 Ctrl+C입니다.<br>
08-1·08-2의 로거는 application.yml의 `SimpleLoggerAdvisor: DEBUG` 설정으로 요청·응답 본문을 출력합니다.<br>
학습용 질문을 사용하며 실제 고객 원문 기록 범위는 서비스 정책에 맞춰 정합니다.

## 결과 해석과 범위

LocalModel은 고정 응답의 연결·파라미터·실행 순서·실제 Java 도구 실행을 검사합니다.<br>
Live는 실제 응답과 기능의 연결을 검사하며, 자연어 문구나 도구 선택 횟수를 고정하지 않습니다.<br>
콘솔의 출력은 assertion을 대신하지 않습니다.<br>
메모리는 단일 서버의 InMemory 저장소이므로 재시작하면 사라집니다.<br>
메모리 서비스의 `synchronized`는 같은 서비스의 동시 실행을 막지만 요청 도착 순서나 분산 제어를 보장하지 않습니다.
