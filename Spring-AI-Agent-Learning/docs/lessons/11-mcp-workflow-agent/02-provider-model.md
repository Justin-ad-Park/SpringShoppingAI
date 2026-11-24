# 11-2 보충. 원격 도구를 모델이 선택하도록 연결하기

**이번 목표:** Spring AI가 제공하는 Provider로 MCP 도구를 ChatClient에 연결하고 모델이 필요한 도구를 선택하게 합니다.<br>
[11-2의 직접 호출](02-direct-call.md)을 배운 뒤, [11-4 Agent 방식](04-agent.md)을 시작하기 전에 읽습니다.

직접 호출에서는 애플리케이션이 `getShippingPolicy`를 지정했습니다.<br>
이번에는 모델에 사용 가능한 도구를 전달하고, “기본 배송비가 얼마인가요?”라는 질문에 필요한 도구를 모델이 선택하게 합니다.<br>
**Provider**는 이 수업에서 MCP 서버의 도구 목록을 읽어 Spring AI가 사용할 `ToolCallback` 목록으로 제공하는 객체입니다.<br>
`ToolCallback`은 도구 설명·입력 형식과 실행 방법을 함께 담는 인터페이스입니다.

[11-1](01-connection.md)의 MCP 서버와 학습 애플리케이션을 실행한 상태에서 아래 API를 호출합니다.

## 제공되는 Provider로 모델에 연결하기

원격 도구를 모델이 선택하게 연결하는 최소 코드는 [Lesson02BasicMcpChatService](../../../labs/11-00-mcp-workflow-agent/src/main/java/com/example/ailearning/l11mcp/lesson02_modelcall/Lesson02BasicMcpChatService.java)입니다.<br>
제공되는 `SyncMcpToolCallbackProvider`가 도구 목록을 콜백으로 만들고 Spring AI가 실제 원격 호출을 연결합니다.<br>
아래는 전체 Service입니다.

```java
package com.example.ailearning.l11mcp.lesson02_modelcall;

import io.modelcontextprotocol.client.McpSyncClient;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

/** 11-2: 제공되는 Provider로 MCP 도구를 모델에 연결한다. 자체 실행기를 사용하지 않는다. */
@Service
public class Lesson02BasicMcpChatService {
    private final ChatClient chatClient;
    private final SyncMcpToolCallbackProvider toolProvider;

    public Lesson02BasicMcpChatService(ChatClient.Builder builder, List<McpSyncClient> mcpClients) {
        chatClient = builder.build();
        toolProvider = SyncMcpToolCallbackProvider.builder()
                .mcpClients(mcpClients)
                .build();
    }

    public String ask(String question) {
        return chatClient
                .prompt()
                .user(question)
                .toolCallbacks(toolProvider)
                .call()
                .content();
    }

    public ToolCallback[] availableTools() {
        return toolProvider.getToolCallbacks();
    }
}
```

```bash
curl http://127.0.0.1:8106/api/mcp/basic -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '기본 배송비가 얼마인가요?'
```

이 요청은 실제 채팅 모델과 MCP 서버를 사용합니다.<br>
모델을 호출하지 않는 [11-2의 도구 목록·배송 정책 조회 API](02-direct-call.md)와 구분하세요.<br>
[로컬 테스트](../../../labs/11-00-mcp-workflow-agent/src/test/java/com/example/ailearning/l11mcp/lesson06_comparison/Lesson06McpComparisonLocalModelTest.java)의 `제공_Provider만으로_원격_도구_결과를_모델에_전달한다`는 도구 이름을 Provider에서 얻어 고정 모델이 선택하게 하고, 실제 MCP 결과가 다음 모델 입력에 들어갔는지 검사합니다.

## 직접 호출과 비교하기

| 구분 | 11-2 직접 호출 | 이번 모델 연결 |
|---|---|---|
| 도구 선택 | Controller가 도구 이름을 지정 | 질문을 받은 모델이 선택 |
| 모델 요청 | 없음 | 도구 선택·최종 답변에 사용 |
| 원격 실행 | 애플리케이션의 콜백과 MCP 클라이언트 | 애플리케이션의 콜백과 MCP 클라이언트 |

11-4에서는 이 기본 연결을 바탕으로 도구 호출 기록과 상한을 추가해 비교합니다.<br>
원격 도구를 사용하는 모든 서비스에 자체 실행기를 만들어야 하는 것은 아닙니다.

## 확인 질문

직접 호출 API와 같은 배송 정책을 조회하더라도 이번 요청에 모델 호출이 필요한 이유는 무엇인가요?

<details>
<summary>해설</summary>

이번에는 도구 이름을 코드에서 고정하지 않고, 모델이 질문에 필요한 도구를 선택하며 도구 결과로 답변을 작성하기 때문입니다.<br>
실제 원격 호출은 두 방식 모두 애플리케이션의 콜백과 MCP 클라이언트가 수행합니다.

</details>

[직접 호출로 돌아가기](02-direct-call.md) · [다음: Agent 방식](04-agent.md) · [11단원 목차](../11-mcp-workflow-agent.md)
