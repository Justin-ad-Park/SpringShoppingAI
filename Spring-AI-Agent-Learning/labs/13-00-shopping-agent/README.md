# 실습 13: 작은 쇼핑 상담 Agent

[교재·실행·검증 안내](../../docs/lessons/13-shopping-agent.md)를 먼저 읽습니다.
애플리케이션은 다른 단원·기존 쇼핑 서비스에 의존하지 않습니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:test --tests com.example.ailearning.l13shopping.ShoppingAgentLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:bootRun
```

로컬 테스트는 외부 유료 API를 호출하지 않습니다. 실제 실행은 저장소 루트 `.env`를 읽습니다. 필요한 설정값과 한계는 교재를 확인하세요.

## 처음 실행할 테스트

먼저 `ShoppingAgentLocalModelTest`의 `상담_흐름은_대화_저장이나_별도_실행기_없이_직접_호출할_수_있다` 메서드를 실행하세요. [교재의 단계별 안내](../../docs/lessons/13-shopping-agent.md#세-단계로-실행하기)에 해당 테스트만 실행하는 명령이 있습니다.

조건 추출·상품/정책 도구·결과 검증을 이해한 다음 후속 대화 테스트를 읽고, 마지막으로 모델/도구 상한과 시간 초과 테스트를 읽습니다. `support`의 스레드·취소 구현은 마지막 단계의 보충입니다.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다. 이 의존성은 테스트에만 적용됩니다. 각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `ShoppingAgentLocalModelTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `ShoppingAgentOpenAiLiveTest`: 실제 OpenAI에 연결합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 테스트 시작 시 빈 줄과 `===== [ 테스트 메서드명 ] ======`을 한 번 출력하고, 그 아래에서 응답·검증 대상 값을 확인합니다. 출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:test --tests com.example.ailearning.l13shopping.ShoppingAgentLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:test --tests com.example.ailearning.l13shopping.ShoppingAgentOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다. 클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.
Live 예제는 실제 응답을 기다릴 수 있도록 테스트 클래스에서 채팅 요청 대기를 30초로 설정합니다. 13장의 전체 실행 대기는 90초입니다. 로컬 실패 테스트와 애플리케이션 기본 한도는 그대로 유지합니다.
정책 문서를 벡터 저장소에 넣기 위해 임베딩 API도 호출합니다. 기본 모델은 `text-embedding-3-small`이며, 필요하면 루트 `.env`의 `OPENAI_EMBEDDING_MODEL`로 바꿀 수 있습니다. 문서 적재 시 외부 임베딩 API 사용량이 발생합니다.
