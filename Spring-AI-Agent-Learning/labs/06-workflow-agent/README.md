# 실습 06: Workflow / Agent / Hybrid

[학습 문서](../../docs/lessons/06-workflow-agent.md)를 먼저 읽으세요.
05의 상품 데이터와 업무 코드를 이 모듈에 복사했으며 다른 실습을 import하지 않습니다.
읽는 순서: Product/Offer/ProductCatalog 복습 → GiftCriteria → ComparisonService의 extract/workflow/agent/hybrid → TracedShoppingTools → 필요 시 support.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:06-workflow-agent:test --tests com.example.ailearning.l06decisions.WorkflowAgentContractTest
./gradlew :Spring-AI-Agent-Learning:labs:06-workflow-agent:bootRun
```

포트 8096. POST `/api/compare/workflow`, `/api/compare/agent`, `/api/compare/hybrid`는 text/plain 질문을 받습니다.
응답의 status와 steps, clientCalls/modelCalls/toolCalls를 비교하세요.
bootRun은 루트 `.env`의 OPENAI_API_KEY와 OPENAI_MODEL을 읽습니다. 로컬 테스트에는 실제 키가 필요 없습니다.
SDK 호출 타임아웃과 전체 요청 대기 상한은 교재에 설명되어 있습니다. 실제 모델에 따라 TIMEOUT이 날 수 있으며 성공처럼 취급하지 않습니다.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다. 이 의존성은 테스트에만 적용됩니다. 각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `WorkflowAgentContractTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `WorkflowAgentOpenAiLiveTest`: 실제 OpenAI에 연결합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 `[한글 테스트 메서드명]`과 응답·검증 대상 값을 함께 확인합니다. 출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:06-workflow-agent:test --tests com.example.ailearning.l06decisions.WorkflowAgentContractTest
./gradlew :Spring-AI-Agent-Learning:labs:06-workflow-agent:test --tests com.example.ailearning.l06decisions.WorkflowAgentOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다. 클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.
Live 예제는 실제 응답을 기다릴 수 있도록 테스트 클래스에서 채팅 요청 대기를 30초로 설정합니다. 06·12의 전체 실행 대기는 90초입니다. 로컬 실패 테스트와 애플리케이션 기본 한도는 그대로 유지합니다.
