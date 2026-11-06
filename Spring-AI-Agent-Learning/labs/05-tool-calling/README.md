# 실습 05: Tool Calling

[교재·실행 예시](../../docs/lessons/05-tool-calling.md)를 먼저 읽습니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:05-tool-calling:test --tests com.example.ailearning.l05tools.ToolCallingLocalModelTest --tests com.example.ailearning.l05tools.ProductCatalogUnitTest
./gradlew :Spring-AI-Agent-Learning:labs:05-tool-calling:bootRun
```

포트 8095. GET `/api/tools/direct`는 Java 검색, GET `/api/tools/shipping/P02`는 Java 배송비 조회,
POST `/api/tools/chat`은 모델이 도구를 선택하는 경로입니다. POST 본문은 text/plain입니다.
bootRun은 루트 `.env`의 OPENAI_API_KEY와 OPENAI_MODEL을 읽습니다. 도구를 지원하는 모델을 선택하세요.
이 모듈은 다른 실습·쇼핑 서비스와 컴파일 의존성이 없습니다.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다. 이 의존성은 테스트에만 적용됩니다. 각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `ProductCatalogUnitTest`: 상품 검색 조건·상품 ID 검증과 무료 배송 계산을 확인하는 단위 테스트입니다. Spring·HTTP 서버·외부 모델 없이 실행합니다.
- `ToolCallingLocalModelTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `ToolCallingOpenAiLiveTest`: 실제 OpenAI에 연결합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 테스트 시작 시 빈 줄과 `===== [ 테스트 메서드명 ] ======`을 한 번 출력하고, 그 아래에서 응답·검증 대상 값을 확인합니다. 출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:05-tool-calling:test --tests com.example.ailearning.l05tools.ToolCallingLocalModelTest --tests com.example.ailearning.l05tools.ProductCatalogUnitTest
./gradlew :Spring-AI-Agent-Learning:labs:05-tool-calling:test --tests com.example.ailearning.l05tools.ToolCallingOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다. 클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.

Spy 생성 조건과 실제 도구 호출 순서·인자 검증은 [05-4 선택 보충](../../docs/lessons/05-tool-calling/04-spy-and-inorder.md)에서 설명합니다.
