# 실습 09: 임베딩과 검색

[교재·실행·검증 안내](../../docs/lessons/09-embeddings-search.md)를 먼저 읽습니다.
이 모듈은 자기 패키지만 사용하며 다른 단원/기존 쇼핑 서비스에 의존하지 않습니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:09-embeddings-search:test --tests com.example.ailearning.l09search.SearchContractTest
./gradlew :Spring-AI-Agent-Learning:labs:09-embeddings-search:bootRun
```

로컬 테스트는 외부 유료 API를 호출하지 않습니다. 실제 실행은 저장소 루트 `.env`를 읽습니다. 필요한 설정값과 한계는 교재를 확인하세요.

## 테스트 선택과 결과 확인

- `SearchContractTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `SearchOpenAiLiveTest`: 실제 OpenAI에 연결합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 `[한글 테스트 메서드명]`과 응답·검증 대상 값을 함께 확인합니다. 출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:09-embeddings-search:test --tests com.example.ailearning.l09search.SearchContractTest
./gradlew :Spring-AI-Agent-Learning:labs:09-embeddings-search:test --tests com.example.ailearning.l09search.SearchOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다. 클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.
임베딩에 `OPENAI_EMBEDDING_MODEL`도 필요합니다. 문서 적재 시에도 외부 임베딩 API 사용량이 발생합니다.
