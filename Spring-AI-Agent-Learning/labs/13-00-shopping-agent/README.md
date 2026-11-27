# 13 기본 쇼핑 상담

[교재](../../docs/lessons/13-shopping-agent.md)의 13-1~4를 실행합니다.<br>
일반 ChatClient로 의도 추출 → 도구 선택 → 실제 조회 결과 검증 → 대화 저장을 연결합니다.

| 수업 | 코드 | 검증 |
|---|---|---|
| 13-1·2 | [Lesson02CounselWorkflow](src/main/java/com/example/ailearning/l13shopping/lesson02_intent/Lesson02CounselWorkflow.java) | [Lesson04ShoppingAgentLocalModelTest](src/test/java/com/example/ailearning/l13shopping/lesson04_conversation/Lesson04ShoppingAgentLocalModelTest.java): 동기 직접 호출·도구 제공 범위 · [Live](src/test/java/com/example/ailearning/l13shopping/lesson04_conversation/Lesson04ShoppingAgentOpenAiLiveTest.java) |
| 13-3 | [Lesson03CounselResultVerifier](src/main/java/com/example/ailearning/l13shopping/lesson03_verification/Lesson03CounselResultVerifier.java) | 같은 테스트의 미확인 ID·예산 초과 거부 |
| 13-4 | [Lesson04CounselService](src/main/java/com/example/ailearning/l13shopping/lesson04_conversation/Lesson04CounselService.java) | 같은 테스트의 조건 유지·실패·세션 격리·삭제 |

패키지의 `lessonNN_주제`와 클래스의 `LessonNN`은 해당 소주제 번호입니다.<br>
공통 Application·Controller·도메인·자료형·테스트 보조 코드는 역할 이름을 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:test --tests '*LocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:bootRun
```

로컬 테스트는 외부 모델을 호출하지 않습니다.<br>
[Live](src/test/java/com/example/ailearning/l13shopping/lesson04_conversation/Lesson04ShoppingAgentOpenAiLiveTest.java)와 앱은 루트 `.env`의 키·채팅 모델·임베딩 모델을 사용하며 시작 시 문서 임베딩부터 실제 API를 호출합니다.<br>
포트는 8102이며 요청 예시는 교재 13-4에 있습니다.<br>
종료는 Ctrl+C입니다.

전체 실행 제한·호출 추적은 별도 [13-8](../13-08-limited-agent/README.md)에 있습니다.<br>
기본 앱 응답에는 trace가 없습니다.

## 선택 실습: 13-9 복합 요청의 누락 확인

[교재](../../docs/lessons/13-shopping-agent/09-result-completeness.md)를 읽고 다음 코드를 확인합니다.<br>
기본 앱의 HTTP API는 유지하며, 선택 실습은 테스트에서 직접 호출합니다.

| 역할 | 코드 |
|---|---|
| 구현 | [Lesson09CompleteCounselService](src/main/java/com/example/ailearning/l13shopping/lesson09_completeness/Lesson09CompleteCounselService.java) |
| 고정 응답 모델 테스트 | [Lesson09CompleteCounselLocalModelTest](src/test/java/com/example/ailearning/l13shopping/lesson09_completeness/Lesson09CompleteCounselLocalModelTest.java) |
| 실제 OpenAI 테스트 | [Lesson09CompleteCounselOpenAiLiveTest](src/test/java/com/example/ailearning/l13shopping/lesson09_completeness/Lesson09CompleteCounselOpenAiLiveTest.java) |
| Java 누락 검사 | [Lesson09ResultCompletenessChecker](src/main/java/com/example/ailearning/l13shopping/lesson09_completeness/Lesson09ResultCompletenessChecker.java) · [단위 테스트](src/test/java/com/example/ailearning/l13shopping/lesson09_completeness/Lesson09ResultCompletenessCheckerUnitTest.java) |

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:test --tests '*Lesson09CompleteCounselLocalModelTest'
```

Live 클래스는 별도 선택하며, 루트 `.env`의 OpenAI 설정과 실제 API 사용량이 필요합니다.

## 선택 실습: 13-10 상품 추천과 반품 안내

[13-10 교재](../../docs/lessons/13-shopping-agent/10-shopping-and-return.md)를 읽고, 13-9와 같은 질문에서 두 조회가 어떻게 실행되는지 확인합니다.

| 역할 | 코드 |
|---|---|
| 구현 | [Lesson10ShoppingAndReturnService](src/main/java/com/example/ailearning/l13shopping/lesson10_shoppingandreturn/Lesson10ShoppingAndReturnService.java) |
| 로컬 검증 | [Lesson10ShoppingAndReturnLocalModelTest](src/test/java/com/example/ailearning/l13shopping/lesson10_shoppingandreturn/Lesson10ShoppingAndReturnLocalModelTest.java) |
| 실제 OpenAI 검증 | [Lesson10ShoppingAndReturnOpenAiLiveTest](src/test/java/com/example/ailearning/l13shopping/lesson10_shoppingandreturn/Lesson10ShoppingAndReturnOpenAiLiveTest.java) |

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:test --tests '*Lesson10ShoppingAndReturnLocalModelTest'
```

## 선택 실습: 13-11 모델이 질문하고 도구를 선택하는 쇼핑 Agent

[13-11 교재](../../docs/lessons/13-shopping-agent/11-model-led-shopping-agent.md)를 따라 모델에 도구와 이전 대화를 전달하는 흐름을 확인합니다.

| 역할 | 코드 |
|---|---|
| 모델 요청과 답변 | [Lesson11ShoppingAgent](src/main/java/com/example/ailearning/l13shopping/lesson11_agent/Lesson11ShoppingAgent.java) |
| 상품·배송비·반품 정책 조회 | [Lesson11ShoppingTools](src/main/java/com/example/ailearning/l13shopping/lesson11_agent/Lesson11ShoppingTools.java) |
| 로컬 검증 | [Lesson11ShoppingAgentLocalModelTest](src/test/java/com/example/ailearning/l13shopping/lesson11_agent/Lesson11ShoppingAgentLocalModelTest.java) |
| 실제 OpenAI 검증 | [Lesson11ShoppingAgentOpenAiLiveTest](src/test/java/com/example/ailearning/l13shopping/lesson11_agent/Lesson11ShoppingAgentOpenAiLiveTest.java) |

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:test --tests '*Lesson11ShoppingAgentLocalModelTest'
```

루트 `.env`를 설정한 뒤 Live를 별도로 실행하면 실제 채팅·임베딩 API를 사용합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:test --tests '*Lesson11ShoppingAgentOpenAiLiveTest'
```
