# 07-2. 메시지 조회·전달·저장 연결하기

**실행 코드·설정·테스트:** [labs/07-chat-memory](../../../labs/07-chat-memory/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

30~45분을 기준으로 한 번에 한 가지 흐름만 읽습니다. 단원 전체 시간은 목차의 기존 예상 시간에 포함됩니다.

**이번 목표:** memory.get → messages → call → memory.add를 찾습니다.

**코드 읽기 안내:** 전체 클래스는 참고 코드입니다. 첫 읽기에서는 chat()의 이전 메시지 조회와 성공 후 저장만 따라갑니다.

## 코드 읽는 순서

모듈: `labs/07-chat-memory`, 패키지: `com.example.ailearning.l07memory`.

1. [MessageView](../../../labs/07-chat-memory/src/main/java/com/example/ailearning/l07memory/MessageView.java), [MemoryReply](../../../labs/07-chat-memory/src/main/java/com/example/ailearning/l07memory/MemoryReply.java): HTTP에서 관찰할 record
2. [MemoryChatService](../../../labs/07-chat-memory/src/main/java/com/example/ailearning/l07memory/MemoryChatService.java): 조회 → 프롬프트 → 호출 → 성공 시 저장
3. [MemoryController](../../../labs/07-chat-memory/src/main/java/com/example/ailearning/l07memory/MemoryController.java): 세션 소유자와 대화 ID 연결

### 서비스 전체 코드

```java
package com.example.ailearning.l07memory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;

@Service
public class MemoryChatService {
    private static final String SYSTEM = """
            쇼핑 대화를 돕습니다. 전달된 이전 메시지와 현재 질문을 참고하세요.
            제공된 정보가 없으면 모른다고 말하세요. 실제 상품 조회 기능은 없습니다.
            """;
    private final ChatClient client;
    private final ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(6).build();

    public MemoryChatService(ChatClient.Builder builder) { client = builder.build(); }

    // 입문용 단일 서버: 같은 대화의 동시 저장 충돌을 막기 위해 요청을 직렬 처리한다.
    public synchronized MemoryReply chat(String owner, String conversationId, String question) {
        String key = key(owner, conversationId);
        UserMessage current = new UserMessage(question);
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM));
        messages.addAll(memory.get(key)); // 1. 이전 질문·응답 조회
        messages.add(current);            // 2. 새 질문 추가

        String answer = client.prompt().messages(messages).call().content(); // 3. 함께 전달
        if (answer == null || answer.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "빈 모델 응답은 저장하지 않습니다.");
        }
        // 4. 성공한 질문·응답만 저장. 이 다음 호출에서 재사용한다.
        memory.add(key, List.of(current, new AssistantMessage(answer)));
        return new MemoryReply(answer, messages.stream().map(MessageView::from).toList(), memory.get(key).size());
    }

    public String stateless(String question) {
        // 메모리 없는 비교 경로: 이전 요청을 조회하거나 저장하지 않는다.
        return client.prompt().system(SYSTEM).user(question).call().content();
    }

    public synchronized List<MessageView> history(String owner, String conversationId) {
        return memory.get(key(owner, conversationId)).stream().map(MessageView::from).toList();
    }

    public synchronized void clear(String owner, String conversationId) {
        memory.clear(key(owner, conversationId));
    }

    private String key(String owner, String conversationId) {
        // owner는 요청 본문이 아닌 서버 세션에서 가져온다.
        return owner + ":" + conversationId;
    }
}
```

핵심은 `memory.get(key)` → `messages.addAll(...)` → `client.prompt().messages(messages)` → `memory.add(...)`입니다.
메모리를 만들어 놓는 것만으로 ChatClient에 연결되지는 않습니다. 이번에는 코드가 명시적으로 연결합니다.
저장 전에 모델 응답을 확인하므로 실패한 질문만 대화에 남지 않습니다.

`synchronized`는 이 단일 서버의 메모리 요청을 한 번에 하나씩 처리하여 동시 저장 충돌을 줄이는 일반 Java 장치입니다.
모든 대화가 직렬 처리되어 처리량이 제한됩니다. 분산 서버·영구 저장·세션 만료 정리·대화 수 제한은 구현하지 않은 학습용 구성입니다.

## 한 번의 후속 요청 처리

```mermaid
sequenceDiagram
    actor User as 사용자
    participant App as Java MemoryChatService
    participant Memory as Spring AI ChatMemory
    participant AI as Spring AI ChatClient
    participant Model as 외부 모델
    User->>App: 세션 쿠키와 대화 ID 및 새 질문
    App->>Memory: get(소유자와 대화 ID로 만든 키)
    Memory-->>App: 이전 user와 assistant 메시지
    App->>App: system과 이전 메시지와 새 user 구성
    App->>AI: messages로 프롬프트 전달
    AI->>Model: 이번 호출의 모든 메시지
    Model-->>AI: 새 답변
    AI-->>App: 답변 문자열
    alt 비어 있지 않은 성공 응답
        App->>Memory: add(새 user와 새 assistant)
        Memory-->>App: 최근 메시지 창 유지
        App-->>User: 답변과 전송 메시지 목록
    else 실패 또는 빈 응답
        App-->>User: 오류 반환하고 저장하지 않음
    end
```

system은 매 요청에서 한 번 추가하고 메모리에는 저장하지 않습니다. 같은 원칙이 중복 저장되는 일을 피합니다.
API 응답의 `sentMessages`는 이번 프롬프트에 직접 구성한 메시지입니다. 테스트는 **실제 모델 API로 전달된 메시지**도 별도로 확인합니다.
실제 서비스에서는 프롬프트 전체를 사용자에게 노출할지 별도로 결정해야 합니다. 이 로컬 수업은 원리 관찰을 위해 반환합니다.


## 여기서 멈춰 확인하기

코드를 닫고 “이번 입력 → 새로 하는 처리 → 확인할 결과”를 한 문장으로 적어 보세요.
설명하기 어렵다면 위 핵심 메서드만 다시 읽습니다. 테스트용 HTTP 서버 구현을 이해해야 다음으로 갈 수 있는 것은 아닙니다.

[이전](01-messages.md) · [다음](03-isolation.md) · [단원 목차](../07-chat-memory.md) · [학습 방법](../../learning-guide.md)
