# 05 보충 5. Spring AI로 도구 호출 횟수 제한하기

**목표:** 도구를 반복 요청하는 모델에 대해 도구별·전체 실행 횟수를 제한하고, 상한에 도달한 결과를 처리합니다.

선수 수업은 05-2의 도구 제공과 05-3의 도구 결과 전달입니다. 기본 예제를 바꾸지 않고 [05-05-tool-call-limits](../../../labs/05-05-tool-call-limits/README.md)를 실행합니다.

## 어떤 횟수를 제한할까?

모델이 같은 검색을 계속 요청하면 비용과 대기가 늘어날 수 있습니다. 프롬프트에 “반복하지 마세요”라고 쓰는 것과 별도로, Spring AI가 도구 실행 전에 횟수를 확인하도록 설정합니다.

| 용어 | 의미 |
|---|---|
| 도구별 상한 | 한 사용자 요청을 처리할 때 같은 도구를 실행할 수 있는 최대 횟수 |
| 전체 상한 | 같은 요청에서 실행하는 모든 도구 횟수의 합계 |
| 모델 요청 횟수 | 모델 서버에 보낸 요청 수. 한 모델 응답에 여러 도구 요청이 있을 수 있어 도구 실행 횟수와 다릅니다. |

## 설정으로 상한 정하기

실습의 `application.yml`에 다음 설정이 있습니다. Spring AI **2.0.2 기본 기능**이며 직접 카운터를 구현하지 않습니다.

```yaml
spring:
  ai:
    tools:
      limits:
        max-calls-per-tool-default: 2
        max-total-tool-calls: 3
        on-limit-exceeded: THROW
```

검색만 반복하면 두 번까지 실행합니다. 검색→배송비→검색까지 세 번 실행한 뒤 배송비를 한 번 더 요청하면 전체 상한에서 차단합니다. 다음 사용자 요청에서는 횟수를 새로 계산합니다.

이 제한은 Spring AI의 도구 실행 경로에 적용됩니다. Workflow에서 `productCatalog.search(...)`를 직접 호출하는 일반 Java 코드는 이 설정의 적용 대상이 아닙니다.

```mermaid
sequenceDiagram
    participant App as ToolLimitService
    participant AI as Spring AI
    participant Model as 모델 서버
    participant Tool as ShoppingTools
    App->>AI: 목표와 도구를 제공해 요청
    loop 검색 도구 실행 2회
        AI->>Model: 질문 또는 이전 도구 결과
        Model-->>AI: searchProducts 실행 요청
        AI->>Tool: searchProducts 실행
        Tool-->>AI: 검색 결과
    end
    AI->>Model: 두 번째 검색 결과
    Model-->>AI: 세 번째 searchProducts 요청
    AI->>AI: 도구별 상한 초과 확인
    AI-->>App: finishReason이 toolCallLimitExceeded인 응답
```

세 번째 검색은 실행하지 않고, 그 뒤 모델에 결과를 다시 보내는 호출도 하지 않습니다. 이 그림의 모델 서버는 로컬 테스트에서 반복 응답하도록 설정합니다.

## 상한 초과를 성공 답변과 구분하기

[ToolLimitService.java](../../../labs/05-05-tool-call-limits/src/main/java/com/example/ailearning/l05limits/ToolLimitService.java)의 전체 코드입니다.

```java
package com.example.ailearning.l05limits;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallLimitExceededException;
import org.springframework.stereotype.Service;

@Service
public class ToolLimitService {
    public record Reply(String status, String answer, String finishReason) {}
    private final ChatClient chatClient;
    private final ShoppingTools shoppingTools;

    public ToolLimitService(ChatClient.Builder builder, ShoppingTools shoppingTools) {
        this.chatClient = builder.build();
        this.shoppingTools = shoppingTools;
    }

    public Reply ask(String question) {
        var response = chatClient
                .prompt()
                .system("필요한 도구로 상품 가격과 배송비를 확인해 답하세요. 일반 인사에는 도구가 필요하지 않습니다.")
                .user(question)
                .tools(shoppingTools)
                .call()
                .chatResponse();
        String finishReason = response.getResult().getMetadata().getFinishReason();
        if (ToolCallLimitExceededException.FINISH_REASON.equals(finishReason)) {
            return new Reply("TOOL_LIMIT", "도구 호출 상한에 도달했습니다.", finishReason);
        }
        return new Reply("COMPLETED", response.getResult().getOutput().getText(), finishReason);
    }
}
```

`THROW`이면 내부 `ToolCallingManager`가 상한 초과 예외를 발생시키지만, 기본 도구 루프의 `ToolCallingAdvisor`가 이를 받아 특별한 종료 사유를 담은 응답으로 반환합니다. 따라서 이 예제에서는 서비스의 `catch` 대신 **`finishReason`을 확인**합니다. `TOOL_LIMIT`은 이를 사용자 응답으로 구분하기 위해 실습에서 정한 상태 이름입니다.

`.content()`만 읽으면 제한 안내 문장도 정상 답변처럼 취급하기 쉽습니다. `.chatResponse()`로 응답의 내용과 종료 사유를 함께 받습니다. 정상 요청에는 `COMPLETED`, 상한 초과에는 `TOOL_LIMIT`을 반환합니다.

도구의 전체 구현은 [ShoppingTools.java](../../../labs/05-05-tool-call-limits/src/main/java/com/example/ailearning/l05limits/ShoppingTools.java), HTTP 진입점은 [ToolLimitController.java](../../../labs/05-05-tool-call-limits/src/main/java/com/example/ailearning/l05limits/ToolLimitController.java)에서 확인합니다.

## 실행하고 비교하기

저장소 루트에서 실행합니다. 로컬 테스트는 API 키 없이 고정 응답 서버를 사용합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:05-05-tool-call-limits:test --tests '*ToolLimitLocalModelTest'
```

| 테스트 입력 상황 | 도구 실제 실행 | 모델 요청 | 결과 |
|---|---|---|---|
| 검색만 계속 요청 | 검색 2회 | 3회 | TOOL_LIMIT |
| 검색→배송비→검색→배송비 요청 | 검색 2회, 배송비 1회 | 4회 | TOOL_LIMIT |
| 검색→배송비→최종 답변 | 검색 1회, 배송비 1회 | 3회 | COMPLETED |
| 인사에 바로 답변 | 0회 | 1회 | COMPLETED |

[ToolLimitLocalModelTest](../../../labs/05-05-tool-call-limits/src/test/java/com/example/ailearning/l05limits/ToolLimitLocalModelTest.java)는 `@MockitoSpyBean`의 호출 기록으로 실제 실행 수를 검증합니다. 고정 모델이 반복 요청했다는 설정과, 도구가 실제 몇 번 실행됐는지는 다른 확인입니다. 테스트 이름과 결과는 콘솔에 출력됩니다.

실제 모델로 실행하려면 루트 `.env`에 `OPENAI_API_KEY`, `OPENAI_MODEL`을 준비합니다. API 사용량이 발생합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:05-05-tool-call-limits:bootRun
curl http://127.0.0.1:8110/api/tool-limits \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '음료 상품 가격과 배송비를 알려줘.'
```

[ToolLimitOpenAiLiveTest](../../../labs/05-05-tool-call-limits/src/test/java/com/example/ailearning/l05limits/ToolLimitOpenAiLiveTest.java)에는 다음 두 사례가 있습니다.

| 사례 | 실행 조건과 확인할 결과 |
|---|---|
| 정상 완료 | 모델이 검색·배송비 도구를 선택하고 `COMPLETED`로 답변 |
| 도구 호출 초과 | 음료·생활·간식의 개별 검색을 요청해, 실제 검색 2회 뒤 `TOOL_LIMIT`으로 종료 |

초과 사례의 질문은 다음과 같습니다.

```text
음료, 생활, 간식 세 카테고리를 각각 검색해서 결과를 하나로 정리해줘.
검색 도구는 한 번에 카테고리 하나씩 호출해줘. 배송비 조회는 필요 없어.
```

하나의 카테고리를 받는 `searchProducts(category)`를 여러 카테고리에 적용하므로 같은 도구를 세 번 사용할 이유가 생깁니다. 샘플 데이터에는 음료의 허브티, 생활의 텀블러, 간식의 쿠키가 있습니다. 별도의 `toolChoice` 강제 옵션은 사용하지 않습니다.

실제 모델은 호출 순서나 묶음을 다르게 선택할 수 있습니다. Live에서는 실제 검색이 두 번 실행된 뒤 제한 상태로 종료되는지 확인하고, 로컬 테스트에서는 음료→생활→간식 요청을 고정해 세 번째 검색이 차단되는 흐름을 재현합니다. 한 모델 응답에 여러 도구 요청이 포함돼도 실제 도구별 실행 상한은 동일하게 적용됩니다.

현재 서비스는 상한에 걸리면 `TOOL_LIMIT`을 반환합니다. 앞서 조회한 두 결과를 합쳐 최종 답변으로 제공하는 기능은 이 예제에 포함하지 않습니다.

설정을 도구별 1회로 바꾼 뒤 **애플리케이션을 재시작**하고 같은 입력을 비교하세요. 실행 후 설정을 2회로 되돌립니다. 로컬 테스트도 같은 설정 파일을 읽으므로 실험 뒤 원복해야 표의 기대값과 맞습니다.

## 확인 질문

1. 세 번째 검색 요청이 있어도 실제 검색은 왜 두 번인가요?
2. 상한 초과를 일반 성공과 구분하는 응답 값은 무엇인가요?
3. 모델 요청 횟수까지 2회로 제한되는 설정인가요?

상한을 넘는 도구 실행은 시작 전에 차단합니다. `finishReason=toolCallLimitExceeded`로 구분하며, 모델 요청 횟수 자체를 제한하는 설정은 아닙니다.

[Spring AI 공식 도구 제한](https://docs.spring.io/spring-ai/reference/api/tools.html#_tool_call_limits) · [이전: Spy 검증](04-spy-and-inorder.md) · [05장 목차](../05-tool-calling.md) · [관련: 모델 타임아웃·재시도](../02-first-call/05-model-timeout-retry.md) · [12장에서 재활용](../12-evaluation/05-failures.md)
