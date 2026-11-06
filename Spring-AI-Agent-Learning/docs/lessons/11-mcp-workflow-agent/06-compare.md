# 11-6. 같은 요청으로 세 방식 비교하기

목표는 **최종 답변 문장뿐 아니라 실제 도구 인자·결과로 보장 범위를 비교하는 것**입니다.

| 항목 | Workflow | Agent | 하이브리드 |
|---|---|---|---|
| 검색 조건 | 모델 추출 후 Java 사용 | 모델이 호출 인자 구성 | 추출·검증한 값으로 Java가 고정 |
| 도구 순서 | Java 코드 | 모델 선택 | 모델 선택, 실행 전 Java 검사 |
| 검색 결과 전체 계산 | Java가 ID 목록 전달 | 프롬프트로 요청 | Java가 ID 목록 검사 |
| 최종 금액 표시 | MCP 결과로 Java 작성 | 모델 답변 | MCP 결과로 Java 작성 |
| 총예산 검사 | Java | 모델 지시 | Java |

## 기록에서 확인하기

세 API의 `toolCalls`는 실제 성공한 MCP 호출의 이름·전송 인자·결과입니다. 예상 순서를 출력한 문자열이 아닙니다.
실패하거나 상한에서 차단된 호출은 성공 기록에 포함되지 않습니다.
기본 상품에서는 검색 결과의 P004·P005가 계산 인자의 `additionalProductIds`에 모두 들어가는지 확인합니다.

## 로컬 통합 테스트

[테스트 전체 소스](../../../labs/11-mcp-workflow-agent/src/test/java/com/example/ailearning/l11mcp/McpComparisonLocalModelTest.java)를 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:11-mcp-workflow-agent:test --tests '*LocalModelTest' --tests '*UnitTest'
```

| 고정 모델로 재현하는 상황 | 확인하는 동작 |
|---|---|
| SearchConditionsUnitTest의 가격·카테고리 경계 검증 | Spring·모델·MCP 없이 객체 규칙 확인 |
| 모델 없이 배송 정책 조회 | 실제 MCP 연결, 모델 요청 0회 |
| Workflow 조건 추출 | 검색 → 두 상품 ID 계산, 총액 25,400원 |
| Agent 도구 선택 | MCP 결과가 다음 모델 입력에 포함 |
| 모델의 조건 변경·잘못된 최종 문장 | 하이브리드 조건 고정과 실제 금액 표시 |
| 일부 상품만 계산 | 실행 전 거부 |
| 총예산 20,000원 | OVER_BUDGET |
| 정보 부족·검색 결과 없음 | 불필요한 후속 호출 생략 |
| 인사·무한 검색 요청 | 도구 생략·호출 상한 |
| 계산 생략·MCP 도구 실패·공백 질문 | UNVERIFIED·도구 오류 전파·입력 거부 |

테스트의 MCP는 `McpTestServer`가 실제 `shopping-mcp`를 임의 포트로 시작합니다. OpenAI에만 고정 HTTP 응답 서버를 사용합니다. 준비된 MCP 서버를 수동 실행할 필요는 없습니다.
이 테스트는 모델의 선택 능력이 아니라, 그 선택을 가정했을 때 애플리케이션과 MCP가 올바르게 연결되는지를 검증합니다.

## 실제 OpenAI 테스트

루트 `.env` 설정을 준비한 뒤 [Live 클래스](../../../labs/11-mcp-workflow-agent/src/test/java/com/example/ailearning/l11mcp/McpComparisonOpenAiLiveTest.java)를 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:11-mcp-workflow-agent:test --tests '*OpenAiLiveTest'
```

실제 모델 선택은 비결정적이므로 테스트가 실패하면 출력의 도구 인자와 결과부터 확인합니다. 금액 assertions는 고정된 서버 상품·배송 정책 기준입니다.
전체 `test`에도 Live가 포함되므로 로컬 검증은 반드시 클래스 필터를 사용합니다.

## 실패와 제한

도구 호출은 요청당 `learning.mcp.max-tool-calls`(기본 6회)로 제한합니다. MCP 요청 한 번은 3초, 모델 요청 한 번은 15초의 대기 제한이 있습니다.
이 제한은 전체 요청의 절대 마감이나 외부 서버 작업의 취소 보장을 뜻하지 않습니다.
검증 거부·호출 상한·연결 실패는 이 실습의 기본 Spring 오류 처리로 HTTP 500이 됩니다. 상세 오류 처리 확장은 다음 평가·운영 단원에서 다룹니다.
클라이언트 초기화 중 서버가 없으면 애플리케이션 시작이 실패할 수도 있습니다. 주소·8082 포트와 서버 실행 여부를 먼저 확인하세요.

## 확인 질문

MCP에 연결했다는 사실만으로 Agent라고 할 수 있을까요?

<details><summary>해설</summary>
아닙니다. MCP는 연결 방식입니다. 어떤 도구를 다음에 사용할지 모델이 판단하는지 실행 코드를 봐야 합니다.
</details>


[11장 목차](../11-mcp-workflow-agent.md)
