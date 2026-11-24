# 09~15단원 소주제별 코드 이름 정리

확인일: 2026-11-24.<br>
08단원의 번호별 예제 배치를 하네스에 명시하고, 09~15단원에 적용했습니다.

## 적용 범위

- 소주제 전용 코드는 `lessonNN_주제` 패키지와 `LessonNN역할` 클래스 이름을 사용합니다.<br>
  예: 09-1의 `lesson01_vectors.Lesson01EmbeddingService`, 15-6의 `lesson06_fields.Lesson06OrderContextDecisionService`.
- 기존 Java 파일 80개의 이름·위치를 정리하고, 관련 import·생성자·문서 코드·소스 링크·테스트 선택 명령을 갱신했습니다.
- 14·15단원의 통합 테스트 클래스 네 개를 소주제별 17개 클래스로 나눴습니다.<br>
  기존 테스트 메서드 22개와 검증문을 유지했으며, 여러 API에 공통인 입력 검증은 공통 클래스로 남겼습니다.
- Application·공통 Controller·도메인·자료형·설정·테스트 보조 코드는 역할 이름을 유지했습니다.<br>
  패키지가 나뉘면서 다른 소주제에서도 사용하는 메서드·질문 상수·테스트 대역의 접근 범위만 필요한 만큼 공개했습니다.
- 기존 README의 대응표를 갱신하거나 없는 곳에 추가했습니다.<br>
  여러 소주제에서 재사용하는 구현은 원래 소주제에 두고 관련 문서에서 연결했습니다.

## 동작 보존 확인

마지막 커밋의 원본과 이동 후 코드를 대조했습니다.<br>
패키지·import·타입 이름·필요한 접근 범위·설명 주석을 제외하면, 이동한 클래스의 처리 로직은 동일합니다.<br>
분리한 테스트의 입력과 검증문도 대조했습니다.

15-6의 로컬 응답 데이터 한 곳은 기존 코드부터 선택지와 불일치했습니다.<br>
서비스의 선택지는 `ORDER_PAYMENT`·`EXPLAIN_RETURN`·`OTHER_QUESTION`인데, 대역의 확률에는 예전 이름인 `CHECK_DELIVERY`·`ASK_CLARIFICATION`이 남아 있어 HTTP 500으로 실패했습니다.<br>
확률의 키를 현재 선택지에 맞췄으며, 고객 질문·기대 결과·assertion은 유지했습니다.

## 실행 검증

전체 `testClasses`로 실제 모델용 테스트를 포함한 컴파일을 확인했습니다.<br>
변경한 모듈에서는 아래 필터로 로컬·단위 테스트만 실행했습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:09-00-embeddings-search:test --tests '*LocalModelTest' --tests '*UnitTest'
```

같은 필터를 아래 모듈에도 적용했습니다.<br>
결과는 Gradle의 `build/test-results/test/TEST-*.xml`에서 집계했습니다.

| 모듈 | 통과한 테스트 |
|---|---:|
| 09-00-embeddings-search | 9 |
| 10-00-rag-advisor | 5 |
| 10-03-custom-rag | 6 |
| 11-00-mcp-workflow-agent | 16 |
| 12-00-evaluation | 8 |
| 12-02-model-evaluation | 4 |
| 12-04-observability | 2 |
| 13-00-shopping-agent | 8 |
| 13-05-intent-classification | 19 |
| 13-08-limited-agent | 11 |
| 14-00-jev-decisions | 25 |
| 15-00-ollama-decisions | 11 |
| 합계 | 124 |

실패·오류·건너뛴 테스트는 각각 0개입니다.<br>
09~15단원의 실행 모듈 12개는 `bootJar`도 통과했습니다.<br>
13-6은 테스트 전용 모듈이므로 기존 설정대로 실행 JAR를 만들지 않습니다.

문서의 로컬 링크와 `git diff --check`도 확인했습니다.<br>
후속 요청으로 실제 Ollama `d1-omni:600m`에 연결해 15단원의 Live 테스트를 실행했습니다.<br>
`./gradlew :Spring-AI-Agent-Learning:labs:15-00-ollama-decisions:test --tests '*LiveTest' --rerun-tasks`로 5개 모두 통과했으며 실패·오류·건너뜀은 각각 0개입니다.<br>
실제 OpenAI·Jev를 호출하는 Live 테스트는 실행하지 않았습니다.<br>
하네스를 새 세션의 에이전트에게 적용하는 행동 평가는 실행하지 않았으며, 위 결과는 실제 교육 코드와 문서의 검증 결과입니다.
