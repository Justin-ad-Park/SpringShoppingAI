# 13-8 상담 전체 실행 제한과 추적

[교재](../../docs/lessons/13-shopping-agent/08-execution-limits.md)를 읽고 [기본 상담](../13-00-shopping-agent/README.md) 이후 실행합니다.<br>
기본 코드와 의존성 없이 별도 패키지로 실행하는 확장입니다.

| 읽을 코드 | 확인할 책임 |
|---|---|
| [Lesson08CounselService](src/main/java/com/example/ailearning/l13limited/lesson08_limits/Lesson08CounselService.java) | 상담 이력 관리와 Workflow 연결 |
| [Lesson08CounselResultVerifier](src/main/java/com/example/ailearning/l13limited/lesson08_limits/Lesson08CounselResultVerifier.java) | 조회 결과에 근거한 답변 검증 |
| [Lesson08CounselWorkflow](src/main/java/com/example/ailearning/l13limited/lesson08_limits/Lesson08CounselWorkflow.java) | 기본 상담 + 계측·제한 응답 처리 |
| [Lesson08LimitedAgentRequestExecutor](src/main/java/com/example/ailearning/l13limited/lesson08_limits/Lesson08LimitedAgentRequestExecutor.java) | 전체 대기·취소 |
| [RunState](src/main/java/com/example/ailearning/l13limited/support/RunState.java) | 모델 요청 시도 수·마감·trace |
| [로컬 테스트](src/test/java/com/example/ailearning/l13limited/lesson08_limits/Lesson08LimitedShoppingAgentLocalModelTest.java) | 상담·모델 상한·도구 상한·늦은 답변 미저장 |
| [Live](src/test/java/com/example/ailearning/l13limited/lesson08_limits/Lesson08LimitedShoppingAgentOpenAiLiveTest.java) | 실제 도구 선택과 후속 대화 |

패키지의 `lessonNN_주제`와 클래스의 `LessonNN`은 해당 소주제 번호입니다.<br>
공통 Application·Controller·도메인·자료형·테스트 보조 코드는 역할 이름을 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-08-limited-agent:test --tests '*LocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:13-08-limited-agent:bootRun
```

로컬 테스트는 외부 모델을 호출하지 않습니다.<br>
도구 상한을 4회와 1회로 비교하려면 [반복 검색 테스트 실습](../../docs/lessons/13-shopping-agent/08-execution-limits.md#도구-호출-상한-4회와-1회-비교하기)을 따라갑니다.<br>
앱과 Live는 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_EMBEDDING_MODEL`을 읽고 실제 채팅·임베딩 API를 호출합니다.<br>
포트 8118의 `/api/counsel/demo`로 기본 상담과 같은 질문을 보냅니다.<br>
종료는 Ctrl+C입니다.
