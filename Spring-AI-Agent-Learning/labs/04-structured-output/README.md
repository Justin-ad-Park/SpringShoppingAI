# 실습 04: 구조화 응답

[교육 문서와 전체 실행 안내](../../docs/lessons/04-structured-output.md)를 먼저 읽습니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:04-structured-output:test --tests com.example.ailearning.l04structured.StructuredOutputContractTest
./gradlew :Spring-AI-Agent-Learning:labs:04-structured-output:bootRun
```

포트 8094. POST `/api/criteria`는 record, POST `/api/criteria/text`는 원문 텍스트를 반환합니다.
두 API 모두 text/plain 질문을 받습니다. 실제 실행에는 루트 `.env`의 OPENAI_API_KEY, OPENAI_MODEL이 필요합니다.
애플리케이션은 다른 단원·기존 쇼핑 서비스에 의존하지 않습니다.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다. 이 의존성은 테스트에만 적용됩니다. 각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `StructuredOutputContractTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `StructuredOutputOpenAiLiveTest`: 실제 OpenAI에 연결합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 `[한글 테스트 메서드명]`과 응답·검증 대상 값을 함께 확인합니다. 출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:04-structured-output:test --tests com.example.ailearning.l04structured.StructuredOutputContractTest
./gradlew :Spring-AI-Agent-Learning:labs:04-structured-output:test --tests com.example.ailearning.l04structured.StructuredOutputOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다. 클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.
