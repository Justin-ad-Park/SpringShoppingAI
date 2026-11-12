# 06 Workflow·Agent·혼합 방식

[06장 교육 문서](../../docs/lessons/06-workflow-agent.md)를 따라 같은 쇼핑 요청의 처리 방식을 비교합니다.
기본 모듈은 직접 만든 횟수 계수·시간 제한·스레드 실행 코드를 사용하지 않습니다. 금액·카테고리 검증과 결과 없음 처리는 업무 요구사항으로 유지합니다.

## 로컬 검증

저장소 루트에서 실행합니다. 실제 외부 API를 호출하지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:06-00-workflow-agent:test --tests '*BudgetProductToolsUnitTest' --tests '*WorkflowAgentLocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:06-00-workflow-agent:bootJar
```

## 실제 모델 연결

루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`을 사용합니다. API 사용량이 발생합니다. IntelliJ의 Working directory도 저장소 루트로 설정합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:06-00-workflow-agent:bootRun
curl http://127.0.0.1:8096/api/compare/WORKFLOW \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '배송비 포함 3만원 이하 음료를 추천해줘.'
./gradlew :Spring-AI-Agent-Learning:labs:06-00-workflow-agent:test --tests '*WorkflowAgentOpenAiLiveTest'
```

경로의 `WORKFLOW`를 `AGENT`, `HYBRID`로 바꿔 같은 고객 요청을 비교합니다. 응답은 `mode`, `status`, `answer`입니다.
`COMPLETED`는 실행 완료이며, 상품 존재나 최종 답변의 사실성을 검증했다는 뜻은 아닙니다.
예산 누락은 `NEEDS_INPUT`, Workflow의 후보 없음은 `NO_RESULTS`, 실행 예외는 `FAILED`입니다.
Hybrid는 빈 조회 결과도 모델이 설명하므로 그 경우 `COMPLETED`입니다.

Java에서는 `service.run(ExecutionMode.WORKFLOW, question)`으로 실행 방식을 지정합니다.
Controller는 `@PathVariable ExecutionMode mode`로 받습니다. URL과 응답 모두 `WORKFLOW`, `AGENT`, `HYBRID` 대문자를 사용하며 소문자나 정의하지 않은 값은 HTTP 400입니다.

### 실제 모델 테스트에서 비교하는 요청

기본 질문은 “친구에게 줄 음료 선물을 찾고 있어요. 배송비까지 3만 원 안에서 추천해 주세요.”입니다.
고객 질문에는 도구명이나 상품 ID, 도구 호출 순서를 넣지 않습니다.

| 테스트 | 검증 내용 |
|---|---|
| Workflow 선물 추천 | 조건 해석 후 검색 → P01 계산 → P02 계산의 고정 순서 |
| Agent 선물 추천 | 실제 검색·배송비 계산 실행, 순서와 반복 횟수는 모델 선택 |
| Hybrid 선물 추천 | 확정 카테고리·예산으로 조회 도구 실행 |
| Workflow 예산 누락 | 조회 전에 추가 입력 요청 |
| Agent 상품 이름 질문 | 검색 실행, 불필요한 배송비 조회 없음 |
| Agent 인사 | 상품 조회 없이 응답 |
| Hybrid 천 원 예산 | 빈 검색 결과를 설명, 가격 계산 없음 |

각 테스트는 공통 메서드에서 요청·결과와 `ToolCallPrinter.printMockitoDetails(productCatalog)` 호출 기록을 출력합니다.
`@MockitoSpyBean`은 실제 카탈로그를 사용하면서 실행 기록을 남깁니다.
Agent의 `ProductTools.searchProducts`·`calculateShipping`은 각각 카탈로그의 `search`·`calculatePriceDetails`를 호출합니다.
Hybrid의 `BudgetProductTools.findProductsWithinBudget`는 고정 조건으로 검색·가격 계산 후 총액을 필터링합니다.
도구는 조회 결과를 반환하며 추천 목록을 저장하는 상태를 갖지 않습니다. `ProductPriceDetail`은 상품 한 건의 금액 정보입니다.

Live 테스트는 실제 모델의 해석·도구 사용을 검사합니다. 모델이 필요한 조회를 생략하면 실패할 수 있습니다.
예산 필터·경계값은 `BudgetProductToolsUnitTest`, 모델에 전달되는 상품·금액과 도구 연결은 `WorkflowAgentLocalModelTest`로 별도 검증합니다.
이 검사는 최종 자연어 답변 전체의 사실성 평가까지 포함하지 않습니다.

검증문이 낯설면 [06장 보충: 테스트 검증 코드 읽기](../../docs/lessons/06-workflow-agent/04-test-assertions.md)를 참고하세요.
모델 HTTP 요청 수는 로컬 테스트의 `MODEL.chat().getRequests()`로 확인합니다. 기본 고정 시나리오는 Workflow 2회, Agent 4회, Hybrid 3회이며 실제 모델의 횟수는 달라질 수 있습니다.

실행 제한은 앞 단원의 보충 실습에서 확인합니다. 각각 별도 모듈입니다.

- [도구 호출 횟수 제한](../05-05-tool-call-limits/README.md)
- [모델 요청 타임아웃과 재시도](../02-05-model-timeout-retry/README.md)
