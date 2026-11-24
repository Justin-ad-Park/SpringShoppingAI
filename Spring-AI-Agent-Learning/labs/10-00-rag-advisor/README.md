# 실행 실습: 표준 Reader·Splitter·RAG Advisor

## 소수업과 실행 코드

| 수업 | 구현 | 검증 |
|---|---|---|
| 10-1 적재 | [Lesson01PolicyDocumentIndexer](src/main/java/com/example/ailearning/l10advisor/lesson01_indexing/Lesson01PolicyDocumentIndexer.java) | [Lesson01PolicyIndexingLocalModelTest](src/test/java/com/example/ailearning/l10advisor/lesson01_indexing/Lesson01PolicyIndexingLocalModelTest.java): 채팅 없이 적재·검색 · [Live](src/test/java/com/example/ailearning/l10advisor/lesson01_indexing/Lesson01PolicyIndexingOpenAiLiveTest.java) |
| 10-2 답변 | [Lesson02AdvisorRagService](src/main/java/com/example/ailearning/l10advisor/lesson02_advisor/Lesson02AdvisorRagService.java) | [Lesson02AdvisorRagLocalModelTest](src/test/java/com/example/ailearning/l10advisor/lesson02_advisor/Lesson02AdvisorRagLocalModelTest.java): 검색 근거가 실제 모델 요청에 포함됨 · [Live](src/test/java/com/example/ailearning/l10advisor/lesson02_advisor/Lesson02AdvisorRagOpenAiLiveTest.java) |

패키지의 `lessonNN_주제`와 클래스의 `LessonNN`은 해당 소주제 번호입니다.<br>
공통 Application·Controller·도메인·자료형·테스트 보조 코드는 역할 이름을 유지합니다.

첫 적재 테스트는 indexing-only 프로필로 채팅용 Service·Controller를 제외합니다.<br>
별도 문서 서버를 띄울 필요 없이 테스트를 실행합니다.


[10-1 문서 적재](../../docs/lessons/10-rag/01-standard-indexing.md) → [10-2 Advisor 연결](../../docs/lessons/10-rag/02-standard-advisor.md) 순서로 읽습니다.<br>
Controller·Service·설정·문서 파일·테스트를 포함한 독립 애플리케이션입니다.<br>
다른 실습 코드를 import하지 않습니다.

저장소 루트에서:

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-00-rag-advisor:test --tests '*LocalModelTest' --tests '*UnitTest'
./gradlew :Spring-AI-Agent-Learning:labs:10-00-rag-advisor:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:10-00-rag-advisor:bootRun
```

로컬 테스트는 고정 응답 HTTP 서버를 사용합니다.<br>
bootRun은 루트 .env의 OPENAI_API_KEY, OPENAI_MODEL, OPENAI_EMBEDDING_MODEL로 실제 호출합니다.<br>
포트 8103, POST /api/advisor-rag, 본문 text/plain.<br>
종료는 Ctrl+C.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다.<br>
이 의존성은 테스트에만 적용됩니다.<br>
각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `Lesson01TokenTextSplitterUnitTest`: 긴 문서의 분할을 확인하는 단위 테스트입니다.<br>
  Spring·HTTP 서버·외부 모델 없이 실행합니다.
- `Lesson02AdvisorRagLocalModelTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `Lesson02AdvisorRagOpenAiLiveTest`: 실제 OpenAI의 비어 있지 않은 응답을 확인합니다.<br>
  검색 문서가 모델 요청에 포함되는지는 로컬 테스트의 메시지 검사로 확인합니다.<br>
  루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 테스트 시작 시 빈 줄과 `===== [ 테스트 메서드명 ] ======`을 한 번 출력하고, 그 아래에서 응답·검증 대상 값을 확인합니다.<br>
  출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-00-rag-advisor:test --tests '*LocalModelTest' --tests '*UnitTest'
./gradlew :Spring-AI-Agent-Learning:labs:10-00-rag-advisor:test --tests com.example.ailearning.l10advisor.lesson02_advisor.Lesson02AdvisorRagOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다.<br>
클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.<br>
임베딩에 `OPENAI_EMBEDDING_MODEL`도 필요합니다.<br>
문서 적재 시에도 외부 임베딩 API 사용량이 발생합니다.
