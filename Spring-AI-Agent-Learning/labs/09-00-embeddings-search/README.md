# 실습 09: 임베딩과 검색

## 소수업과 실행 코드

| 수업 | 구현 | 테스트·확인할 동작 |
|---|---|---|
| 09-1 | [Lesson01EmbeddingService.embed](src/main/java/com/example/ailearning/l09search/lesson01_vectors/Lesson01EmbeddingService.java) | [Lesson01EmbeddingOnlyLocalModelTest](src/test/java/com/example/ailearning/l09search/lesson01_vectors/Lesson01EmbeddingOnlyLocalModelTest.java): 문서 적재 없이 입력 한 번만 임베딩 · [Live](src/test/java/com/example/ailearning/l09search/lesson01_vectors/Lesson01EmbeddingOnlyOpenAiLiveTest.java) |
| 09-2 | [Lesson02BasicVectorSearchService.search](src/main/java/com/example/ailearning/l09search/lesson02_store/Lesson02BasicVectorSearchService.java) | [Lesson02BasicVectorSearchLocalModelTest](src/test/java/com/example/ailearning/l09search/lesson02_store/Lesson02BasicVectorSearchLocalModelTest.java): 적재·query·topK · [Live](src/test/java/com/example/ailearning/l09search/lesson02_store/Lesson02BasicVectorSearchOpenAiLiveTest.java) |
| 09-3 | [Lesson03SearchService.compareSearchResults](src/main/java/com/example/ailearning/l09search/lesson03_search/Lesson03SearchService.java) | [Lesson03SearchLocalModelTest](src/test/java/com/example/ailearning/l09search/lesson03_search/Lesson03SearchLocalModelTest.java): 비교·하한·필터, [Live](src/test/java/com/example/ailearning/l09search/lesson03_search/Lesson03SearchOpenAiLiveTest.java): 실제 임베딩·검색 |

패키지의 `lessonNN_주제`와 클래스의 `LessonNN`은 해당 소주제 번호입니다.<br>
공통 Application·Controller·도메인·자료형·테스트 보조 코드는 역할 이름을 유지합니다.

[교재·실행·검증 안내](../../docs/lessons/09-00-embeddings-search.md)를 먼저 읽습니다.<br>
애플리케이션은 다른 단원·기존 쇼핑 서비스에 의존하지 않습니다.<br>
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:09-00-embeddings-search:test --tests com.example.ailearning.l09search.lesson03_search.Lesson03SearchLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:09-00-embeddings-search:bootRun
```

로컬 테스트는 외부 유료 API를 호출하지 않습니다.<br>
실제 실행은 저장소 루트 `.env`를 읽습니다.<br>
필요한 설정값과 한계는 교재를 확인하세요.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다.<br>
이 의존성은 테스트에만 적용됩니다.<br>
각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `Lesson03SearchLocalModelTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `Lesson03SearchOpenAiLiveTest`: 실제 OpenAI에 연결합니다.<br>
  루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 테스트 시작 시 빈 줄과 `===== [ 테스트 메서드명 ] ======`을 한 번 출력하고, 그 아래에서 응답·검증 대상 값을 확인합니다.<br>
  출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:09-00-embeddings-search:test --tests com.example.ailearning.l09search.lesson03_search.Lesson03SearchLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:09-00-embeddings-search:test --tests com.example.ailearning.l09search.lesson03_search.Lesson03SearchOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다.<br>
클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.<br>
임베딩에 `OPENAI_EMBEDDING_MODEL`도 필요합니다.<br>
문서 적재 시에도 외부 임베딩 API 사용량이 발생합니다.
