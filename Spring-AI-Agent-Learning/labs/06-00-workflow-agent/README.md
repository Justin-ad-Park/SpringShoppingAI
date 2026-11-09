# 06 Workflow·Agent·혼합 방식

[06장 교육 문서](../../docs/lessons/06-workflow-agent.md)를 따라 같은 쇼핑 요청의 처리 방식을 비교합니다.
기본 모듈은 직접 만든 횟수 계수·시간 제한·스레드 실행 코드를 사용하지 않습니다. 금액·카테고리 검증과 결과 없음 처리는 업무 요구사항으로 유지합니다.

## 로컬 검증

저장소 루트에서 실행합니다. 실제 외부 API를 호출하지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:06-00-workflow-agent:test --tests '*WorkflowAgentLocalModelTest'
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

경로의 `WORKFLOW`를 `AGENT`, `HYBRID`로 바꿔 비교합니다. 응답은 `mode`, `status`, `answer`, `offers`입니다. Agent의 offers는 실제 조회한 금액이며 최종 예산 준수는 보장하지 않습니다. 혼합 방식은 조회 사실을 다시 검증해 확정한 후보만 반환합니다.

Java에서는 `service.run(ExecutionMode.WORKFLOW, question)`처럼 Enum으로 실행 방식을 지정합니다. `ExecutionMode`는 정해진 선택지인 `WORKFLOW`, `AGENT`, `HYBRID`를 하나의 타입으로 묶습니다. Controller는 `@PathVariable ExecutionMode mode`로 값을 직접 받고, 서비스는 Enum 상수로 분기합니다. URL과 JSON 응답도 같은 대문자 이름을 사용하므로 별도의 대소문자 변환 메서드가 필요 없습니다. 소문자나 정의하지 않은 값은 Spring MVC가 HTTP 400으로 거부합니다. 예를 들어 응답은 `"mode":"WORKFLOW"` 형태입니다.

`WorkflowAgentOpenAiLiveTest`는 같은 질문을 세 개의 독립 테스트로 비교합니다.

| 테스트 | 검증·출력 내용 |
|---|---|
| Workflow | Java가 실행하는 `search(음료, 30000)` → P01 가격 계산 → P02 가격 계산 순서와 최종 P01 후보 검증 |
| Agent | 모델이 선택한 도구가 실제로 수행한 검색·계산 기록 출력. 선택 순서·횟수를 고정하지 않음 |
| 혼합 방식 | 실제 검색의 고정 조건과 최종 후보의 조회 여부·카테고리·배송비 포함 예산 검증, 실제 호출 기록 출력 |

세 테스트는 `ProductCatalog`에 `@MockitoSpyBean`을 두고 `ToolCallPrinter.printMockitoDetails(productCatalog)`를 출력합니다. Workflow는 카탈로그를 직접 호출하며, Agent·혼합 방식의 도구는 내부에서 같은 카탈로그를 호출합니다. 05장과 같은 도구 이름인 `searchProducts`·`calculateShipping`을 사용하며, 내부에서 카탈로그의 `search`·`calculatePriceDetails`를 각각 호출합니다. Agent 테스트는 두 카탈로그 메서드가 각각 한 번 이상 실행됐는지 검증합니다. 도구 객체 자체의 호출 인자 기록과는 다르며, 특히 혼합 방식에서는 Java가 고정한 검색 인자가 출력됩니다.
`InOrder`는 이미 실행된 기록을 검증하며 실행 순서를 지정하지 않습니다. 실제 모델이 필요한 조회를 생략하거나 조건 추출에 실패하면 Live 테스트도 실패할 수 있습니다.

테스트의 `extracting`, `containsExactly`, `allSatisfy`, `verify`, `atLeastOnce`가 낯설면 [06장 보충: 테스트 검증 코드 읽기](../../docs/lessons/06-workflow-agent/04-test-assertions.md)를 참고하세요.

모델 호출 수는 로컬 테스트 서버의 요청 목록으로 확인합니다. 자체 RunState나 CountedChatModel은 사용하지 않습니다.

실행 제한은 앞 단원의 보충 실습에서 확인합니다. 각각 별도 모듈입니다.

- [도구 호출 횟수 제한](../05-05-tool-call-limits/README.md)
- [모델 요청 타임아웃과 재시도](../02-05-model-timeout-retry/README.md)
