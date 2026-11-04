# 실행 실습: Spring AI Evaluator

[11-2 교재](../../docs/lessons/11-evaluation/02-model-judge.md)를 읽습니다.
이 모듈은 다른 실습과 독립이며 실행 애플리케이션·설정·테스트를 포함합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:11-model-evaluation:test --tests com.example.ailearning.l11judge.EvaluationApiContractTest
./gradlew :Spring-AI-Agent-Learning:labs:11-model-evaluation:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:11-model-evaluation:bootRun
```

저장소 루트에서 실행합니다. test는 로컬 고정 모델 서버를 사용하고,
bootRun은 루트 .env의 OPENAI_API_KEY, OPENAI_MODEL로 실제 평가 호출을 수행합니다.
포트 8104, POST /api/evaluate/relevance 또는 /facts, JSON의 question·context·answer를 전달합니다. 종료는 Ctrl+C.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다. 이 의존성은 테스트에만 적용됩니다. 각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `EvaluationApiContractTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `EvaluationApiOpenAiLiveTest`: 실제 OpenAI에 연결합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 `[한글 테스트 메서드명]`과 응답·검증 대상 값을 함께 확인합니다. 출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:11-model-evaluation:test --tests com.example.ailearning.l11judge.EvaluationApiContractTest
./gradlew :Spring-AI-Agent-Learning:labs:11-model-evaluation:test --tests com.example.ailearning.l11judge.EvaluationApiOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다. 클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.
