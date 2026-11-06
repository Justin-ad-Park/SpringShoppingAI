# 11-00-mcp-workflow-agent

[11장 교재](../../docs/lessons/11-mcp-workflow-agent.md)의 세 방식은 같은 shopping-mcp 서버의 검색·계산 도구를 사용합니다.

## 설정과 실행

Java 21·Spring AI 2.0.2는 루트 빌드에서 관리합니다. 루트 `.env`에 `OPENAI_API_KEY`, `OPENAI_MODEL`을 준비합니다.
실행 위치는 저장소 루트입니다. 수동 실행은 터미널 두 개에서 각각 수행합니다.

```bash
./gradlew :shopping-mcp:bootRun
```

```bash
./gradlew :Spring-AI-Agent-Learning:labs:11-00-mcp-workflow-agent:bootRun
```

MCP는 기본 8082 포트, 학습 API는 8106 포트입니다. 종료는 Ctrl+C입니다. 서버 주소는 `LEARNING_MCP_URL`로 변경할 수 있습니다.

```bash
curl http://127.0.0.1:8106/api/mcp/tools
curl http://127.0.0.1:8106/api/mcp/shipping-policy
curl http://127.0.0.1:8106/api/mcp/workflow \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-binary '음료 15000원 이하 모두 한 개씩 구매, 배송비 포함 총예산 30000원'
```

마지막 경로를 `/agent`, `/hybrid`로 바꾸어 비교합니다. 실제 성공한 호출은 `toolCalls`에 표시됩니다.
기본 상품의 상품 합계는 22,400원, 배송비 포함 총액은 25,400원입니다. 실제 장바구니는 변경하지 않습니다.

## 로컬 테스트와 빌드

```bash
./gradlew :Spring-AI-Agent-Learning:labs:11-00-mcp-workflow-agent:test --tests '*LocalModelTest' --tests '*UnitTest'
./gradlew :Spring-AI-Agent-Learning:labs:11-00-mcp-workflow-agent:bootJar
```

`McpComparisonLocalModelTest`는 OpenAI 고정 응답과 실제 MCP 서버를 사용합니다. 키나 수동 서버 실행 없이 동작하며 외부 API를 호출하지 않습니다.
테스트 전용 `McpTestServer`가 shopping-mcp를 임의 포트로 실행·종료합니다. 운영 코드는 shopping-mcp 클래스를 import하지 않습니다.
객체 규칙은 별도 `SearchConditionsUnitTest`에서 검증합니다. 테스트명은 시작 시 한 번 출력하고 결과는 그 아래 출력합니다. 정상·실패와 호출 기록을 assertion으로 검증합니다.

## 실제 모델 테스트

```bash
./gradlew :Spring-AI-Agent-Learning:labs:11-00-mcp-workflow-agent:test --tests '*OpenAiLiveTest'
```

`McpComparisonOpenAiLiveTest`는 실제 OpenAI에 연결하고 MCP 서버는 자동 실행합니다. 전체 test에도 Live가 포함됩니다.
실제 모델의 도구 선택은 고정되지 않으며, 고정 데이터의 금액과 의미 있는 도구 실행을 검증합니다.

## 코드 읽기 순서

ShoppingMcpGateway의 목록·직접 호출 → SearchConditionExtractor → McpWorkflowService → McpAgentService → McpHybridService 순서입니다.
McpToolRun은 기록·상한을 제공하는 보조 코드이며, 조건 고정 부분은 하이브리드 수업에서 읽습니다.
연결 실패는 shopping-mcp 실행 여부와 URL, 포트 충돌을 확인하세요. IDE에서 새 모듈의 Run 아이콘이 없으면 루트 Gradle 프로젝트를 다시 동기화합니다.
