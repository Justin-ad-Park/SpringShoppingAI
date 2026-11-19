# 13-4. 검증 결과를 저장하고 후속 질문 처리하기

**이번 수업의 목표:** 이전 대화로 후속 질문을 해석하는 흐름을 연결하고, 확인된 결과 없음과 처리 실패에서 저장 여부가 다른 이유를 설명합니다.

**실행 코드·설정·테스트:** [13-00-shopping-agent](../../../labs/13-00-shopping-agent/README.md). 07장의 대화 메모리와 13-2·3의 상담·검증 흐름을 사용합니다.

## 후속 질문에는 무엇이 빠져 있을까?

첫 질문 “배송비 포함 3만원 이하 음료 추천”에는 조건이 모두 있지만, 다음 질문 “생활용품으로 바꿔줘”에는 예산이 없습니다. 모델이 이전 질문·답변을 함께 받아야 카테고리만 바꾸고 예산은 유지할 수 있습니다. 모델이 서버의 메모리를 스스로 찾아 읽는 것이 아니라, 애플리케이션이 이전 메시지를 다음 요청에 전달합니다.

| 요청 순서 | 질문 | 유지·변경할 조건 | 이 샘플 데이터로 확인할 결과 |
|---|---|---|---|
| 1 | 배송비 포함 3만원 음료 추천 | 음료·3만원 설정 | P01 허브티 12000원 + 배송비 3000원 = 15000원 등 예산 안의 상품 |
| 2 | 생활용품으로 변경 | 예산 3만원 유지, 카테고리를 생활로 변경 | 생활 상품 P03은 35000원이므로 검색 결과 없음 |
| 3 | 예산을 4만원으로 변경 | 생활 유지, 예산만 4만원으로 변경 | P03 텀블러 35000원 + 배송비 0원 = 35000원 |

두 번째 결과가 없다고 대화를 저장하지 않으면 세 번째 요청에서 생활용품으로 바꿨다는 문맥이 빠집니다. 따라서 **조회하여 결과가 없음을 확인한 대화도 저장**합니다. 위 표는 Java 상품 데이터와 고정 시나리오의 기준이며, 실제 모델의 선택·성공을 보장하는 표는 아닙니다.

## 조회·실행·저장 책임을 나누기

[CounselService](../../../labs/13-00-shopping-agent/src/main/java/com/example/ailearning/l13shopping/CounselService.java)는 다음 순서로 처리합니다.

1. 소유자와 대화 ID를 조합한 키로 `ChatMemory`에서 이전 메시지를 읽습니다.
2. 이전 메시지의 복사본과 현재 질문을 `CounselWorkflow`에 전달합니다. Workflow는 현재 질문을 추가해 의도 추출과 도구 선택에 사용합니다.
3. 검증 결과가 저장 가능한 상태이면 현재 질문을 `UserMessage`, 답변을 `AssistantMessage`로 함께 저장합니다.
4. 응답에는 상담 결과와 저장된 메시지 수를 담습니다.

다음은 실제 `chat()` 메서드 발췌입니다. `counselWorkflow`는 생성자로 주입받고, `conversationMemory`는 아래에서 설명하는 `MessageWindowChatMemory` 필드입니다. 전체 생성자와 조회·삭제 메서드는 소스 링크에서 확인합니다.

```java
public synchronized Reply chat(String owner, String conversationId, String question) {
    String conversationKey = owner + ":" + conversationId;
    List<Message> previousMessages = List.copyOf(conversationMemory.get(conversationKey));
    CounselResult counselResult;
    try {
        counselResult = counselWorkflow.processCounselRequest(question, previousMessages);
    } catch (RuntimeException error) {
        // 실패한 대화는 저장하지 않고 기존 대화는 유지한다.
        log.warn("Counsel failed: errorType={}", error.getClass().getSimpleName());
        return new Reply("FAILED", "상담 처리에 실패했습니다. 이전 대화는 유지합니다.", null,
                List.of(), List.of(), conversationMemory.get(conversationKey).size());
    }
    if (List.of("VERIFIED", "NEEDS_INPUT", "NO_RESULTS", "NO_CONTEXT").contains(counselResult.status())) {
        conversationMemory.add(conversationKey,
                List.of(new UserMessage(question), new AssistantMessage(counselResult.answer())));
    }
    return new Reply(counselResult.status(), counselResult.answer(), counselResult.intent(),
            counselResult.offers(), counselResult.sources(), conversationMemory.get(conversationKey).size());
}
```

메모리에 쓰는 주체를 Service 한 곳으로 모아 상담 실행과 저장 판단을 구분합니다. `synchronized`는 이 Service 객체의 메서드 실행이 겹치지 않게 하는 Java 문법입니다. 모델 호출까지 순서대로 처리하므로 여러 사용자의 처리량에 제약이 있으며, 운영 환경의 분산 동시성 제어를 제공하는 구성은 아닙니다.

## 어떤 대화를 저장할까?

다음 상태 이름은 Spring AI의 표준 상태가 아니라 이 샘플의 업무 처리 결과입니다. 이름 암기보다 저장 이유를 확인하세요.

| 상담 결과 | 저장 여부 | 이유 |
|---|---|---|
| `VERIFIED` | 현재 질문·답변 저장 | 실제 조회 사실과 현재 검증 기준을 통과한 결과를 후속 문맥에 사용 |
| `NEEDS_INPUT` | 저장 | 예산을 묻는 추가 질문을 다음 답변과 연결 |
| `NO_RESULTS`, `NO_CONTEXT` | 저장 | 상품·정책 조회 결과가 없음을 확인했으며 변경한 조건도 다음 요청에 필요 |
| `VERIFY_FAILED` | 저장하지 않음 | 모델 선택과 실제 조회 사실이 일치하지 않음 |
| `NO_VERIFIED_OFFER`, `NO_VERIFIED_SOURCE` | 저장하지 않음 | 필요한 조회·확인이 부족해 결과 없음을 확정할 수 없음 |
| 처리 예외로 반환한 `FAILED` | 저장하지 않음 | 이번 처리를 완료하지 못했으므로 이전의 유효한 대화만 유지 |

실패 시에는 이전 기록을 지우지 않고 이번 질문·답변만 추가하지 않습니다. 검증 상태의 구체적 판정은 [13-3](03-verify.md)을 참고하세요. HTTP 200이어도 `status`가 실패일 수 있으므로 HTTP 코드와 업무 상태를 구분합니다.

## 메모리에 남는 범위

Service가 만드는 메모리 필드 발췌입니다.

```java
private final ChatMemory conversationMemory = MessageWindowChatMemory.builder()
        .maxMessages(6)
        .build();
```

최대 6개는 대화 여섯 번이 아니라 **메시지 여섯 개**입니다. 이 예제는 질문·답변 두 개씩 저장하므로 처음 세 번을 저장하면 `storedMessages=6`입니다. 더 저장하면 오래된 대화가 제거되어 과거 조건을 계속 기억한다고 보장할 수 없습니다. 이 저장소는 프로세스 메모리이므로 재시작하면 대화가 사라집니다. 저장 정책과 저장소 선택은 [07장](../07-chat-memory.md)에서 복습합니다.

소유자는 Controller에서 받은 HTTP 세션 ID이며, 같은 세션에서도 `conversationId`가 다르면 별도 대화입니다. `curl`의 같은 쿠키 파일과 같은 `/demo` 경로를 유지해야 후속 대화가 이어집니다. 세션 쿠키에 의한 구분은 이 실습의 사용자 구분이며 운영 서비스의 인증·인가를 대신하지 않습니다.

## 실행하고 조건 변화 확인하기

명령은 저장소 루트에서 실행합니다. 먼저 외부 API를 호출하지 않는 로컬 테스트로 연결·검증·저장 정책을 확인합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:test --tests '*ShoppingAgentLocalModelTest'
```

실제 실행은 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_EMBEDDING_MODEL`을 사용합니다. [실행 설정 안내](../../run-guide.md)를 참고하세요. 서버 시작 시 정책 문서 임베딩, 정책 검색 시 질문 임베딩, 상담 시 채팅 모델의 API 사용량이 발생할 수 있습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:bootRun
```

다른 터미널에서 같은 쿠키 파일로 순서대로 요청합니다.

```bash
curl -c /tmp/learning13.cookies -b /tmp/learning13.cookies http://127.0.0.1:8102/api/counsel/demo -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '배송비 포함 30000원 음료를 추천해줘'
curl -c /tmp/learning13.cookies -b /tmp/learning13.cookies http://127.0.0.1:8102/api/counsel/demo -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '생활용품으로 바꿔줘'
curl -c /tmp/learning13.cookies -b /tmp/learning13.cookies http://127.0.0.1:8102/api/counsel/demo -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '예산은 40000원으로 바꿔줘'
```

| 응답 필드 | 확인할 내용 |
|---|---|
| `criteria.category`, `criteria.totalBudget` | 음료·30000 → 생활·30000 → 생활·40000으로 바뀌는가 |
| `status` | 조회·검증 결과를 나타내는가. 두 번째 요청의 결과 없음도 저장되는가 |
| `offers` | 세 번째 요청에서 P03의 배송비 포함 총액이 35000원인가 |
| `storedMessages` | 저장 가능한 결과가 세 번이면 2 → 4 → 6인가 |

Java의 `Reply` 접근자는 `intent()`지만 `@JsonProperty("criteria")`로 JSON에는 `criteria`가 출력됩니다. 실제 모델이 실패 상태를 반환했다면 위 증가를 기대하지 말고 상태와 조회·검증 결과부터 확인합니다.

다음 API는 같은 대화의 저장 이력을 조회하고 삭제합니다. 삭제 후 “4만원으로 바꿔줘”만 요청하면 이전 카테고리 문맥이 없다는 차이를 관찰할 수 있습니다. 실제 응답은 모델에 따라 달라지며, 삭제 실험 후에는 완전한 첫 질문으로 다시 시작하세요. 서버 종료는 Ctrl+C입니다.

```bash
curl -b /tmp/learning13.cookies http://127.0.0.1:8102/api/counsel/demo
curl -X DELETE -b /tmp/learning13.cookies http://127.0.0.1:8102/api/counsel/demo
```

[ShoppingAgentLocalModelTest](../../../labs/13-00-shopping-agent/src/test/java/com/example/ailearning/l13shopping/ShoppingAgentLocalModelTest.java)의 `후속_질문에서_예산을_유지하고_결과_없는_조건도_기억한다`는 카테고리·예산·P03·저장 메시지 수를 검증합니다. 실패 시 기존 기록 유지와 세션 격리도 별도 테스트로 확인합니다. 고정 서버의 결과이므로 실제 언어 이해·도구 선택 품질은 [Live 테스트](../../../labs/13-00-shopping-agent/src/test/java/com/example/ailearning/l13shopping/ShoppingAgentOpenAiLiveTest.java)와 별도 질문 세트로 평가합니다.

전체 대기·모델 요청 수 제한, trace와 늦은 답변 미저장은 선택 실습 [13-8](08-execution-limits.md)에서 추가합니다.

## 확인 질문

1. 조회 결과가 없는 카테고리 변경도 저장해야 하는 이유는 무엇인가요?
2. 모델 호출이 실패하면 이번 요청과 이전 대화를 각각 어떻게 처리하나요?
3. `maxMessages(6)`이면 과거의 카테고리·예산을 언제까지나 유지할 수 있나요?

<details>
<summary>해설</summary>

1. 변경한 조건이 다음 질문의 문맥이기 때문입니다. 생활용품 결과가 없다는 대화를 저장해야 다음 “4만원으로 변경”에서도 생활 카테고리를 참고할 수 있습니다. 조회하지 않은 경우는 결과 없음으로 확정하지 않습니다.
2. 이번 질문·답변은 저장하지 않고 이전 대화는 유지합니다. 처리하지 못한 결과를 유효한 후속 문맥으로 남기지 않기 위한 샘플의 정책입니다.
3. 아닙니다. 메시지 여섯 개를 넘으면 오래된 메시지가 제거되며 재시작하면 메모리도 사라집니다. 장기 보존할 업무 조건은 별도 상태·저장소 설계가 필요합니다.

</details>

[이전](03-verify.md) · [목차](../13-shopping-agent.md) · [선택: 실행 제한](08-execution-limits.md)
