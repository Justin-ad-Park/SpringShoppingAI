# 08-2. 메모리와 도구 루프의 위치

30~45분을 기준으로 한 번에 한 가지 흐름만 읽습니다. 단원 전체 시간은 목차의 기존 예상 시간에 포함됩니다.

**이번 목표:** 앞에서 직접 만든 문맥 전달을 Advisor와 연결합니다.

**코드 읽기 안내:** 재귀 Advisor를 직접 작성하지 않습니다. 메모리·A·B가 도구 루프 안팎 중 어디에 놓였는지만 비교합니다.

## 메모리를 연결하고 도구 루프 안팎 비교하기

[AdvisorChatService.java](../../../labs/08-advisors/src/main/java/com/example/ailearning/l08advisors/AdvisorChatService.java)의 전체 코드입니다.

```java
package com.example.ailearning.l08advisors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.memory.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdvisorChatService {
    public record Reply(String answer, List<String> steps, int storedMessages) {}
    private final ChatClient client;
    private final ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(6).build();

    public AdvisorChatService(ChatClient.Builder builder) {
        // 07에서 직접 작성했던 메시지 조회·추가·저장을 메모리 Advisor가 담당한다.
        client = builder.defaultSystem("전달된 대화를 참고해 답하세요. 가격은 제공된 도구 결과만 사용하세요.")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build()).build();
    }

    public synchronized Reply chat(String owner, String conversationId, String question, boolean useTool) {
        var steps = new ArrayList<String>();
        int loopOrder = ToolCallingAdvisor.DEFAULT_ORDER;
        var request = client.prompt().user(question)
                .advisors(new TraceAdvisor("A", loopOrder - 10, steps),
                        new TraceAdvisor("B", loopOrder + 10, steps))
                .advisors(parameters -> parameters.param(ChatMemory.CONVERSATION_ID, owner + ":" + conversationId));
        if (useTool) request.tools(new PriceTools());
        String answer = request.call().content();
        // memory.add()를 다시 호출하면 중복 저장될 수 있으므로 추가하지 않는다.
        return new Reply(answer, List.copyOf(steps), memory.get(owner + ":" + conversationId).size());
    }

    public synchronized void clear(String owner, String conversationId) { memory.clear(owner + ":" + conversationId); }
}
```

`defaultAdvisors`는 모든 요청에 메모리 연결을 적용하고, 요청별 `.advisors(...)`는 이번 관찰 A/B와 대화 ID를 추가합니다.
**07의 memory.add와 이전 메시지 조립을 함께 사용하지 않습니다.** 둘 다 적용하면 중복 저장/전달이 생길 수 있습니다.
소유자는 이전과 같이 서버 세션 ID에서 얻습니다. 이 수업도 단일 서버·메모리 저장·직렬 처리라는 한계가 있습니다.

A는 자동 ToolCallingAdvisor보다 앞에, B는 뒤에 둡니다. 그래서 A는 전체 요청, B는 반복되는 모델 호출을 관찰합니다.

```mermaid
flowchart TD
    M[기본 Memory Advisor: 이전 대화 읽기] --> A[A before: 전체 요청 시작]
    A --> T[ToolCallingAdvisor: 루프 관리]
    T --> B[B before]
    B --> C[외부 모델 요청]
    C --> D[B after]
    D --> Q{모델 응답에 도구 요청이 있는가}
    Q -->|있음| X[Java: 허용된 도구 실행]
    X --> T
    Q -->|없음| R[A after: 최종 응답]
    R --> S[기본 Memory Advisor: 최종 대화 저장]
```

기본 메모리 Advisor는 도구 루프 밖에 있습니다. 이 구성은 user와 최종 assistant만 저장하며 tool 메시지는 저장하지 않습니다.
도구 결과는 같은 요청 안의 반복에는 전달됩니다. “이번 요청에서 사용”과 “다음 대화에도 저장”을 구분하세요.
메모리를 루프 안으로 이동하는 심화 구성은 이번에 구현하지 않습니다.


## 여기서 멈춰 확인하기

코드를 닫고 “이번 입력 → 새로 하는 처리 → 확인할 결과”를 한 문장으로 적어 보세요.
설명하기 어렵다면 위 핵심 메서드만 다시 읽습니다. 테스트용 HTTP 서버 구현을 이해해야 다음으로 갈 수 있는 것은 아닙니다.

[이전](01-chain.md) · [다음 소수업](03-observe.md) · [학습 방법](../../learning-guide.md)
