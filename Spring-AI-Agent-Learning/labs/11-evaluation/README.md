# 실습 11: 평가와 운영 기초

[교재·실행·검증 안내](../../docs/lessons/11-evaluation.md)를 먼저 읽습니다.
애플리케이션은 다른 단원·기존 쇼핑 서비스에 의존하지 않습니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:11-evaluation:test --tests com.example.ailearning.l11evaluation.EvaluationContractTest
./gradlew :Spring-AI-Agent-Learning:labs:11-evaluation:bootRun
```

로컬 테스트는 외부 유료 API를 호출하지 않습니다. 실제 실행은 저장소 루트 `.env`를 읽습니다. 필요한 설정값과 한계는 교재를 확인하세요.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다. 이 의존성은 테스트에만 적용됩니다. 각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `EvaluationContractTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `EvaluationOpenAiLiveTest`: 실제 OpenAI에 연결합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 `[한글 테스트 메서드명]`과 응답·검증 대상 값을 함께 확인합니다. 출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:11-evaluation:test --tests com.example.ailearning.l11evaluation.EvaluationContractTest
./gradlew :Spring-AI-Agent-Learning:labs:11-evaluation:test --tests com.example.ailearning.l11evaluation.EvaluationOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다. 클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.
Live 예제는 실제 응답을 기다릴 수 있도록 테스트 클래스에서 채팅 요청 대기를 30초로 설정합니다. 06·12의 전체 실행 대기는 90초입니다. 로컬 실패 테스트와 애플리케이션 기본 한도는 그대로 유지합니다.
임베딩에 `OPENAI_EMBEDDING_MODEL`도 필요합니다. 문서 적재 시에도 외부 임베딩 API 사용량이 발생합니다.
