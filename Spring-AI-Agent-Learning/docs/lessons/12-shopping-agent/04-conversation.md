# 12-4. 대화 저장과 통합 시나리오 관찰

**실행 코드·설정·테스트:** [labs/12-shopping-agent](../../../labs/12-shopping-agent/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

30~45분을 기준으로 한 번에 한 가지 흐름만 읽습니다. 단원 전체 시간은 목차의 기존 예상 시간에 포함됩니다.

**이번 목표:** 정상 결과·결과 없음·실패에서 저장 여부를 비교합니다.

**코드 읽기 안내:** 동시성 보조 코드부터 구현하지 않습니다. 먼저 세 번의 후속 요청으로 조건이 이어지는지 관찰합니다.

## 4. 대화 저장 시점과 실행 상한

`chat()`은 이전 대화의 복사본으로 작업합니다. 작업 스레드는 메모리를 수정하지 않습니다.
성공·추가 질문·확인된 결과 없음(`VERIFIED`, `NEEDS_INPUT`, `NO_RESULTS`, `NO_CONTEXT`)만
호출자 스레드에서 user/assistant 쌍으로 저장합니다.
유효한 “생활용품으로 변경” 요청의 검색 결과가 없어도 저장해야 다음 “4만원으로 변경”에서 생활 조건을 유지할 수 있습니다.

실패·상한 초과·타임아웃 때는 이번 요청을 저장하지 않습니다. 기존 대화는 유지합니다.
타임아웃 뒤 도착한 응답도 저장하지 않는 것을 테스트합니다.
간단히 관찰하도록 Service를 `synchronized`로 직렬화했습니다. 운영 환경의 동시성 설계는 별도입니다.
메모리는 프로세스 내 최대 6개 메시지이며 재시작하면 사라집니다. 오래된 중요 조건의 영구 보존을 보장하지 않습니다.
쿠키는 학습용 소유자 구분이며 운영 서비스 인증을 대신하지 않습니다.

| 기본 설정 | 값 | 의미 |
|---|---:|---|
| `max-model-calls` | 8 | 의도 추출과 내부 도구 루프를 합친 모델 요청 상한 |
| `max-tool-calls` | 6 | 이번 요청의 실제 도구 진입 상한 |
| `timeout-ms` | 20000 | 실행 작업의 결과를 기다리는 최대 시간 |
| Chat/Embedding `timeout` | 5s | SDK의 요청 시간 제한 |
| Chat/Embedding `max-retries` | 0 | SDK 자동 재시도 없음 |

HTTP 요청의 대기열·전송 시간을 포함한 전체 SLA를 보장하는 설정은 아닙니다.
작업 취소를 요청해도 외부 제공 업체가 이미 수행한 요청과 비용까지 취소되지는 않을 수 있습니다.
`trace.clientCalls`는 최상위 ChatClient 사용 수, `modelCalls`는 내부 도구 루프를 포함한 실제 Chat 요청 수입니다.
임베딩 요청은 이 Chat 계수에 포함하지 않습니다. 기록은 도구 요청과 반환값이며 모델의 내부 사고가 아닙니다.

## 5. 실행과 관찰

먼저 API 키 없이 테스트합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:12-shopping-agent:test :Spring-AI-Agent-Learning:labs:12-shopping-agent:bootJar
```

실제 실행은 저장소 루트 `.env`에 `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_EMBEDDING_MODEL`을 준비한 뒤 실행합니다.
작성 방법은 [실행 설정 안내](../../run-guide.md)를 참고하세요.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:12-shopping-agent:bootRun
```

실제 실행은 시작 시 정책 임베딩, 정책 검색 시 질문 임베딩, 상담 시 Chat 요청 비용이 발생할 수 있습니다.
자동 테스트는 고정 응답 서버를 사용하며 실제 API 키를 사용하지 않습니다.

다른 터미널에서 동일 쿠키 파일을 유지합니다.

```bash
curl -s -c /tmp/learning12.cookies -b /tmp/learning12.cookies   http://127.0.0.1:8102/api/counsel/demo   -H 'Content-Type: text/plain; charset=UTF-8'   --data-raw '배송비 포함 30000원 음료를 추천하고 미개봉 반품 기간도 알려줘'

curl -s -c /tmp/learning12.cookies -b /tmp/learning12.cookies   http://127.0.0.1:8102/api/counsel/demo   -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '생활용품으로만 바꿔줘'

curl -s -c /tmp/learning12.cookies -b /tmp/learning12.cookies   http://127.0.0.1:8102/api/counsel/demo   -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '예산을 40000원으로 바꿔줘'

curl -s -b /tmp/learning12.cookies http://127.0.0.1:8102/api/counsel/demo
curl -s -X DELETE -b /tmp/learning12.cookies http://127.0.0.1:8102/api/counsel/demo
```

다음 표는 Java 데이터와 테스트의 고정 시나리오에서 확인한 기준입니다.
실제 모델은 다른 도구 순서·선택 또는 실패 상태를 반환할 수 있습니다.

| 요청 | 확인할 결과 |
|---|---|
| 3만원 음료 + 미개봉 반품 | P01 허브티 12000+배송비 3000=15000원, `return-sealed`의 7일 원문 |
| 생활용품으로 변경 | 예산 3만원 유지. P03 35000원이므로 `NO_RESULTS` |
| 예산 4만원으로 변경 | 생활 조건 유지. P03 텀블러 35000+배송비 0=35000원 |
| 예산 없이 추천 | 도구 실행 전에 `NEEDS_INPUT` |
| 상품가만 가능한 13000원 음료 | P01 배송비 포함 15000원 확인 뒤 추천 불가 |
| 미개봉 반품 기간만 질문 | 예산 없이 정책 도구 사용. 상품 도구는 미제공 |
| 정책 근거가 검색되지 않음 | `NO_CONTEXT`로 보류. 모델 선택·검색 결과에 따라 달라짐 |

`FAILED`, `VERIFY_FAILED`, `NO_VERIFIED_OFFER`, `NO_VERIFIED_SOURCE`는 HTTP 200 응답의 업무 상태입니다.
`MODEL_LIMIT`, `TOOL_LIMIT`, `TIMEOUT`, `CANCELLED`도 응답 `status`로 확인합니다.
공백·2000자 초과 질문과 잘못된 대화 ID는 Controller에서 HTTP 400으로 거부합니다.

## 6. 테스트와 한 가지씩 바꾸는 실험

[ShoppingAgentContractTest](../../../labs/12-shopping-agent/src/test/java/com/example/ailearning/l12shopping/ShoppingAgentContractTest.java)의 9개 테스트는
실제 MVC·Spring AI 어댑터·벡터 저장소에 로컬 가짜 Chat/Embedding HTTP 서버를 연결합니다.

- 상품·배송비·반품 근거 결합과 Java의 최종 표시.
- 후속 카테고리·예산 수정, 결과 없음도 대화에 저장.
- 배송비 때문에 예산을 초과한 전체 검색 결과 처리.
- 예산 누락과 업무별 허용 도구 차이.
- 허위 출처·예산 초과 선택 거부.
- 세션 격리·삭제 범위·실패 시 기존 대화 유지.
- 반복 도구 호출 상한과 타임아웃 이후 미저장.

가짜 서버는 정해진 문장·벡터만 처리합니다. 언어 이해·검색 품질·실제 도구 선택 성능의 증거가 아닙니다.
이 구분은 11장의 모델 품질 평가에서도 유지합니다.

실험은 하나씩 수행하고 원본으로 복원합니다.

1. 메모리를 삭제하고 “4만원으로 바꿔줘”만 질문합니다. 이전 카테고리를 알 수 있는지 확인합니다.
2. 정책 질문으로 시작해 `trace`와 허용 도구가 상품 추천과 어떻게 다른지 비교합니다.
3. `max-tool-calls`를 1로 줄입니다. 여러 조회가 필요한 요청이 완성된 답변처럼 저장되는지 확인합니다.
4. 11장의 고정 평가 방식을 복습해 상품 ID·총액·출처 ID를 함께 평가할 사례 3개를 작성합니다.

## 7. 이해 확인

- 두 프롬프트의 역할과 Java가 고정한 순서를 각각 설명할 수 있나요?
- 검색 결과를 받았는데도 최종 상품·출처 ID를 다시 검사하는 이유는 무엇인가요?
- “결과 없음”과 “조회하지 않음”을 구분해야 하는 이유는 무엇인가요?
- 언제 이전 대화를 저장하고, 실패하면 무엇을 유지하나요?

<details><summary>확인 해설</summary>

첫 프롬프트는 조건 추출, 두 번째는 도구 선택과 최종 ID 선택입니다.
바깥 단계와 허용 도구·금액 검증·저장·상한은 Java가 정합니다.
모델이 조회 결과에 없는 ID를 만들거나 조건을 어길 수 있으므로 실제 사실과 대조합니다.
빈 선택만으로 조회 결과가 없다고 증명할 수 없습니다.
확인된 정상 결과·추가 질문·결과 없음은 다음 대화를 위해 저장하고, 실패 요청은 저장하지 않아 이전 대화를 유지합니다.

</details>

필수 과정은 여기까지입니다. MCP 확장은 로컬 도구를 원격 도구로 바꾸는 선택 수업이며 아직 구현하지 않았습니다.
[교육 계획](../../curriculum.md) · [레퍼런스 안내](../../references.md) · [검증 기록](../../verification.md)

## 여기서 멈춰 확인하기

코드를 닫고 “이번 입력 → 새로 하는 처리 → 확인할 결과”를 한 문장으로 적어 보세요.
설명하기 어렵다면 위 핵심 메서드만 다시 읽습니다. 테스트용 HTTP 서버 구현을 이해해야 다음으로 갈 수 있는 것은 아닙니다.

[이전](03-verify.md) · [다음](../12-shopping-agent.md) · [단원 목차](../12-shopping-agent.md) · [학습 방법](../../learning-guide.md)
