# 실습 12: 평가와 운영 기초

[교재·실행·검증 안내](../../docs/lessons/12-evaluation.md)를 먼저 읽습니다.
애플리케이션은 다른 단원·기존 쇼핑 서비스에 의존하지 않습니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:12-00-evaluation:test --tests com.example.ailearning.l12evaluation.EvaluationLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:12-00-evaluation:bootRun
```

로컬 테스트는 외부 유료 API를 호출하지 않습니다. 실제 실행은 저장소 루트 `.env`를 읽습니다. 필요한 설정값과 한계는 교재를 확인하세요.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다. 이 의존성은 테스트에만 적용됩니다. 각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `EvaluationLocalModelTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `EvaluationOpenAiLiveTest`: 실제 OpenAI에 연결합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 테스트 시작 시 빈 줄과 `===== [ 테스트 메서드명 ] ======`을 한 번 출력하고, 그 아래에서 응답·검증 대상 값을 확인합니다. 출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:12-00-evaluation:test --tests com.example.ailearning.l12evaluation.EvaluationLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:12-00-evaluation:test --tests com.example.ailearning.l12evaluation.EvaluationOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다. 클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.
Live 테스트는 채팅·임베딩의 SDK 요청별 `timeout`을 각각 30초로 설정합니다. 테스트 메서드 전체에는 JUnit의 `@Timeout(180)`을 적용합니다. 요청별 통신 제한과 여러 평가 사례를 실행하는 테스트 전체 제한은 서로 다릅니다. 이 모듈에는 별도의 90초 전체 실행 제한이 없습니다.
임베딩에 `OPENAI_EMBEDDING_MODEL`도 필요합니다. 문서 적재 시에도 외부 임베딩 API 사용량이 발생합니다.
