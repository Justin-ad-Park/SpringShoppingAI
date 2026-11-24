# 실습 10-3~5: 업무 규칙을 직접 구성하는 RAG

[교재·실행·검증 안내](../../docs/lessons/10-rag.md)를 먼저 읽습니다.<br>
[10-1·2의 표준 기능](../10-00-rag-advisor/README.md)을 배운 뒤 제목 기반 적재·빈 검색 시 생성 생략·출처 검증을 추가합니다.<br>
이 앱은 표준 예제와 독립 실행합니다.<br>
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-03-custom-rag:test --tests com.example.ailearning.l10rag.lesson04_generation.Lesson04RagLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:10-03-custom-rag:bootRun
```

로컬 테스트는 외부 유료 API를 호출하지 않습니다.<br>
실제 실행은 저장소 루트 `.env`를 읽습니다.<br>
필요한 설정값과 한계는 교재를 확인하세요.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다.<br>
이 의존성은 테스트에만 적용됩니다.<br>
각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `Lesson04RagLocalModelTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `Lesson04RagOpenAiLiveTest`: 실제 OpenAI의 답변과 출처 목록을 확인합니다.<br>
  문장 전체가 근거와 일치하는지까지 판정하지는 않습니다.<br>
  루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 테스트 시작 시 빈 줄과 `===== [ 테스트 메서드명 ] ======`을 한 번 출력하고, 그 아래에서 응답·검증 대상 값을 확인합니다.<br>
  출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-03-custom-rag:test --tests com.example.ailearning.l10rag.lesson04_generation.Lesson04RagLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:10-03-custom-rag:test --tests com.example.ailearning.l10rag.lesson04_generation.Lesson04RagOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다.<br>
클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.<br>
임베딩에 `OPENAI_EMBEDDING_MODEL`도 필요합니다.<br>
문서 적재 시에도 외부 임베딩 API 사용량이 발생합니다.

## 소주제 번호로 코드 찾기

번호가 붙은 클래스는 처음 소개하는 소주제의 예제입니다.<br>
Application·공통 Controller·도메인·자료형·테스트 보조 코드는 역할 이름을 유지합니다.<br>
후속 소주제는 앞서 만든 클래스를 재사용할 수 있습니다.

| 소주제 | 실행 코드 | 전용 테스트 |
|---|---|---|
| 10-4 | [Lesson04RagService](src/main/java/com/example/ailearning/l10rag/lesson04_generation/Lesson04RagService.java) | [Lesson04RagLocalModelTest](src/test/java/com/example/ailearning/l10rag/lesson04_generation/Lesson04RagLocalModelTest.java) · [Lesson04RagOpenAiLiveTest](src/test/java/com/example/ailearning/l10rag/lesson04_generation/Lesson04RagOpenAiLiveTest.java) |
