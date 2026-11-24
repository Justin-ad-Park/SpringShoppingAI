# 실습 10-3~5: 업무 규칙을 직접 구성하는 RAG

[교재·실행·검증 안내](../../docs/lessons/10-rag.md)를 먼저 읽습니다.<br>
[10-1·2의 표준 기능](../10-00-rag-advisor/README.md)을 배운 뒤 여러 정책이 있는 파일의 제목 기반 적재·관련 항목만 전달·빈 검색 시 생성 생략·출처 검증을 추가합니다.<br>
이 앱은 표준 예제와 독립 실행합니다.<br>
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-03-custom-rag:test --tests '*UnitTest' --tests '*LocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:10-03-custom-rag:bootRun
```

로컬 테스트는 외부 유료 API를 호출하지 않습니다.<br>
실제 실행은 저장소 루트 `.env`를 읽습니다.<br>
필요한 설정값과 한계는 교재를 확인하세요.

## 어떤 내용을 모델에 전달할까?

[배송·주문 혜택 정책](src/main/resources/policies/delivery-policies.md)과 [반품·환불 정책](src/main/resources/policies/return-policies.md)에 여섯 항목이 있고, 각 항목은 여러 문장으로 구성됩니다.<br>
로더는 `policies/` 하위의 모든 `.md` 파일을 찾아 파일별·항목별로 읽습니다.<br>
정책 파일들을 항목별로 나누어 적재한 다음, 고객 질문으로 검색된 항목만 채팅 LLM에 보냅니다.<br>
항목 안의 조건과 예외는 함께 유지하고 모델이 질문에 필요한 답변을 작성하도록 요청합니다.

검색 결과는 `/api/rag/sources?question=...`, 답변 생성은 `/api/rag`에서 확인합니다.<br>
두 API는 각각 검색하므로 같은 질문을 보내도 질문 임베딩이 따로 발생합니다.<br>
[10-4의 실행 예](../../docs/lessons/10-rag/04-custom-generation.md#4-검색-원문과-답변-비교하기)에서 검색 원문과 답변을 비교합니다.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다.<br>
이 의존성은 테스트에만 적용됩니다.<br>
각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `Lesson03PolicyLoaderUnitTest`: 모델 없이 두 정책 파일의 분할·출처·조건과 예외 보존을 확인합니다.
- `Lesson03PolicyJarLoadingUnitTest`: JAR 내부의 하위 폴더 탐색·확장자 필터·상대 출처를 확인하는 보조 테스트입니다.
- `Lesson04RagLocalModelTest`: 고정 벡터와 답변으로 전달할 정책 선택·무관한 항목 제외·정상·실패 처리를 확인합니다.
- `Lesson04RagOpenAiLiveTest`: 검색한 정책 원문·실제 OpenAI의 답변·출처 목록을 출력합니다.
  출력을 위한 별도 검색 때문에 서비스 내부 검색 외에 질문 임베딩이 한 번 더 발생합니다.<br><br>
  문장 전체가 근거와 일치하는지까지 판정하지는 않습니다.<br>
  루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 테스트 시작 시 빈 줄과 `===== [ 테스트 메서드명 ] ======`을 한 번 출력하고, 그 아래에서 응답·검증 대상 값을 확인합니다.<br>
  출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-03-custom-rag:test --tests '*UnitTest' --tests '*LocalModelTest'
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
| 10-3 | [Lesson03PolicyLoader](src/main/java/com/example/ailearning/l10rag/lesson03_indexing/Lesson03PolicyLoader.java) · [Lesson03PolicySearch](src/main/java/com/example/ailearning/l10rag/lesson03_indexing/Lesson03PolicySearch.java) | [Lesson03PolicyLoaderUnitTest](src/test/java/com/example/ailearning/l10rag/lesson03_indexing/Lesson03PolicyLoaderUnitTest.java) · [JAR 호환성](src/test/java/com/example/ailearning/l10rag/lesson03_indexing/Lesson03PolicyJarLoadingUnitTest.java) |
| 10-4 | [Lesson04RagService](src/main/java/com/example/ailearning/l10rag/lesson04_generation/Lesson04RagService.java) | [Lesson04RagLocalModelTest](src/test/java/com/example/ailearning/l10rag/lesson04_generation/Lesson04RagLocalModelTest.java) · [Lesson04RagOpenAiLiveTest](src/test/java/com/example/ailearning/l10rag/lesson04_generation/Lesson04RagOpenAiLiveTest.java) |
