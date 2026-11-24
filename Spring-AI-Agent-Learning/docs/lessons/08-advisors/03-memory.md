# 08-3. 메모리 Advisor로 후속 대화 연결하기

**이번 목표:** 07단원에서 직접 작성한 이전 메시지 조회·전달·저장을 `MessageChatMemoryAdvisor`로 연결하고, 후속 요청에 대화가 전달되는지 확인합니다.

이 수업은 `lesson03_memory` 패키지의 [Lesson03MemoryAdvisorChatService](../../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/lesson03_memory/Lesson03MemoryAdvisorChatService.java)를 읽습니다.<br>
[08-2](02-registration.md)의 기본 등록에 메모리 기능을 연결하는 수업입니다.

## 같은 사용자와 후속 질문 연결하기

다음 두 질문은 같은 사용자의 같은 대화에 속해야 합니다.

```text
첫 질문: 선물 예산은 3만원입니다.
후속 질문: 제가 말한 예산은 얼마인가요?
```

모델은 앞선 요청을 스스로 보관하지 않습니다.<br>
07단원에서는 서비스가 이전 메시지를 읽어 새 질문과 합친 뒤, 정상 답변을 확인하고 대화를 저장했습니다.<br>
이번에는 그 처리를 Spring AI의 기본 제공 `MessageChatMemoryAdvisor`에 맡깁니다.

| 객체 | 이 예제의 역할 |
|---|---|
| `MessageWindowChatMemory` | 최근 메시지를 최대 6개까지 보관하도록 관리 |
| `MessageChatMemoryAdvisor` | 이전 메시지를 프롬프트에 포함하고 현재 대화를 저장 |
| `ChatMemory.CONVERSATION_ID` | 이번 요청에서 사용할 대화를 지정하는 매개변수 키 |

저장소·메시지 창 정책 자체는 [07단원](../07-chat-memory.md)의 기능입니다.<br>
이번에 새로 배우는 부분은 **그 메모리를 ChatClient의 호출 과정에 연결하는 방법**입니다.

## 서비스에서 달라지는 코드

아래는 이 수업의 서비스 전체 코드입니다.<br>
등록과 호출을 분리해서 읽으세요.<br>
`defaultSystem(...)`은 이 ChatClient의 기본 시스템 메시지를 설정합니다.<br>
요청별 `.system(...)`과의 차이는 [03단원의 기본 시스템 메시지 실습](../03-chatclient-and-prompts.md#33-모든-요청에-사용할-기본-시스템-메시지-설정하기)에서 확인할 수 있습니다.

```java
package com.example.ailearning.l08advisors.lesson03_memory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;

/** 08-3: 이전 메시지 조회·전달·저장을 Spring AI의 MessageChatMemoryAdvisor에 맡긴다. */
@Service
public class Lesson03MemoryAdvisorChatService {
    public record Reply(String answer, int storedMessages) {}

    private final ChatClient chatClient;
    private final ChatMemory conversationMemory = MessageWindowChatMemory.builder()
            .maxMessages(6)
            .build();

    public Lesson03MemoryAdvisorChatService(ChatClient.Builder builder) {
        chatClient = builder
                .defaultSystem("이전 대화를 참고해 사용자의 질문에 답하세요.")
                // default: 이 ChatClient의 모든 요청에 메모리 기능을 연결한다.
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(conversationMemory)
                        .build())
                .build();
    }

    // synchronized는 chat()/clear()의 동시 실행을 막는다. 모델 응답을 기다리는 동안 다른 사용자도 대기한다.
    public synchronized Reply chat(String owner, String conversationId, String question) {
        String conversationKey = owner + ":" + conversationId;
        String answer = chatClient
                .prompt()
                .user(question)
                // Advisor를 추가하는 코드가 아니라, 이번 요청에서 사용할 대화 키를 설정한다.
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationKey))
                .call()
                .content();
        // Advisor가 저장하므로 conversationMemory.add()를 다시 호출하지 않는다.
        return new Reply(answer, conversationMemory.get(conversationKey).size());
    }

    public synchronized void clear(String owner, String conversationId) {
        conversationMemory.clear(owner + ":" + conversationId);
    }
}
```

1. 생성자의 `.defaultAdvisors(...)`는 이 ChatClient의 모든 요청에 메모리 Advisor를 적용합니다.
2. `chat()`의 `.advisors(parameters -> ...)`는 이번 요청의 대화 키를 전달합니다.<br>
   여기서는 새 Advisor를 추가하는 것이 아니라 이미 등록한 메모리 Advisor가 읽을 매개변수를 설정합니다.
3. 이전 메시지 조립과 `conversationMemory.add()`는 서비스에서 직접 수행하지 않습니다.<br>
   같은 처리를 다시 넣으면 중복 전달·저장이 생길 수 있습니다.

### param(...)과 params(...)로 설정값 전달하기

위 코드의 `advisorSpec`은 `ChatClient.AdvisorSpec` 타입입니다.<br>
Spring AI가 제공하는 이 객체에 **이번 요청에서 Advisor가 사용할 설정값**을 넣습니다.<br>
다음은 `ChatClient`에서 필요한 선언만 발췌한 코드입니다.

```java
import java.util.Map;

public interface ChatClient {
    interface AdvisorSpec {
        AdvisorSpec param(String k, Object v);
        AdvisorSpec params(Map<String, Object> p);
        // 나머지 선언 생략
    }
    // 나머지 선언 생략
}
```

| 메서드·인자 | 의미 | 이 예제에서 전달하는 값 |
|---|---|---|
| `param(k, v)` | 설정 이름과 값을 한 개씩 전달 | 대화 키 설정 한 개 |
| `k` (`String`) | Advisor가 설정값을 찾을 때 사용하는 이름 | `ChatMemory.CONVERSATION_ID` |
| `v` (`Object`) | 해당 이름에 저장할 값 | `conversationKey` |
| `params(p)` | 설정 이름과 값을 Map에 담아 한 번에 전달 | 위와 같은 설정을 Map으로 전달 가능 |

따라서 아래 코드는 **“이번 요청의 대화 기록은 `conversationKey`로 구분해 사용하세요”**라는 설정을 전달합니다.

```java
// 위 chat() 메서드 안의 요청 구성 부분을 발췌했습니다.
String answer = chatClient
        .prompt()
        .user(question)
        .advisors(advisorSpec -> advisorSpec.param(
                ChatMemory.CONVERSATION_ID, conversationKey))
        .call()
        .content();
```

예를 들어 `conversationKey`가 `"session-1:budget"`이면, 메모리 Advisor는 그 키로 이전 메시지를 조회하고 현재 대화를 저장합니다.<br>
`ChatMemory.CONVERSATION_ID`는 **설정 이름**이고, `conversationKey`는 **실제로 사용할 대화 키의 값**입니다.

같은 설정을 `params(...)`로 전달하려면 `.advisors(...)` 부분을 다음처럼 바꿀 수 있습니다.<br>
`Map`의 import는 `java.util.Map`입니다.

```java
.advisors(advisorSpec -> advisorSpec.params(
        Map.of(ChatMemory.CONVERSATION_ID, conversationKey)))
```

설정 하나에는 `param(...)`이 간단하고, 여러 설정을 Map으로 준비했다면 `params(...)`로 함께 전달할 수 있습니다.<br>
값의 타입이 `Object`여도 어떤 값이든 의미가 있는 것은 아닙니다.<br>
설정 이름과 값의 타입은 사용하는 Advisor가 정한 방식에 맞춰야 합니다.<br>
이 예제의 대화 키는 문자열이며, 모델에 보낼 질문 문장이 아니라 **메모리 Advisor가 대화 기록을 구분하는 데 사용하는 값**입니다.

`owner`는 [AdvisorController](../../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/AdvisorController.java)가 얻은 서버 세션 ID입니다.<br>
같은 `conversationId`라도 사용자 세션이 다르면 다른 대화 키를 만듭니다.<br>
대화 ID만으로 로그인 권한을 확인하는 기능은 아닙니다.<br>
이 실습은 단일 서버의 InMemory 저장소를 사용하며, `synchronized`로 같은 서비스 객체의 `chat()`과 `clear()`가 동시에 실행되지 않게 합니다.<br>
모델 응답을 기다리는 동안 다른 사용자의 요청도 대기하며, 요청 도착 순서를 보장하는 기능은 아닙니다.<br>
여러 서버의 동시 갱신 제어까지 구현한 예제는 아닙니다.

## 람다는 대화 ID를 쓰는 설정 함수다

생성자의 `defaultAdvisors`에는 **메모리 Advisor 객체**가 들어갑니다.<br>
반면 `chat()`의 람다는 **이번 요청에 사용할 대화 키를 설정하는 함수**입니다.<br>
아래는 위 메서드의 설정 부분을 풀어 쓴 동등한 Java 코드입니다.<br>
`Consumer`는 인자 하나를 받아 작업하고 반환값은 주지 않는 함수형 인터페이스입니다.

```java
// chat() 내부의 대화 키 설정을 풀어 쓴 발췌. Consumer는 java.util.function.Consumer입니다.
Consumer<ChatClient.AdvisorSpec> memorySettings = advisorSpec -> {
    advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationKey);
};

String answer = chatClient
        .prompt()
        .user(question)
        .advisors(memorySettings)
        .call()
        .content();
```

여기서 `advisorSpec`은 Spring AI가 설정을 채우도록 전달하는 객체의 변수명입니다.<br>
`.advisors(...)`가 이 함수를 실행해 현재 요청의 설정을 준비합니다.<br>
그 뒤 실제 호출 과정에서 메모리 Advisor가 대화 키를 읽어 이전 대화를 가져옵니다.<br>
대화 키 자체를 사용자 질문에 붙이는 코드가 아니며, 다른 Advisor를 하나 더 등록하지도 않습니다.<br>
람다가 하나라는 사실과 동시 요청 처리는 관련이 없습니다.<br>
동시 실행을 막는 것은 메서드의 `synchronized`입니다.

## 후속 요청에 포함되는 메시지

정상 응답 하나씩을 받는 이 예제의 첫 대화 후에는 user·assistant 두 메시지가 저장됩니다.<br>
두 번째 호출의 모델 입력은 아래와 같습니다.<br>
system 메시지는 서비스의 기본 지시이며, `storedMessages`에는 포함되지 않습니다.

```text
system: 이전 대화를 참고해 사용자의 질문에 답하세요.
user: 선물 예산은 3만원입니다.
assistant: 첫 번째 실제 답변
user: 제가 말한 예산은 얼마인가요?
```

두 번째 답변까지 저장하면 `storedMessages=4`입니다.<br>
대화가 길어지면 메시지 창 정책에 따라 오래된 메시지는 제거됩니다.<br>
`MessageChatMemoryAdvisor`의 등록·대화 ID 사용법은 [Spring AI 공식 설명](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)을 참고합니다.

## 실행하고 테스트와 연결하기

저장소 루트에서 실행합니다.<br>
LocalModel은 외부 API를 호출하지 않고, Live는 루트 `.env`로 실제 OpenAI를 호출합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*Lesson03MemoryAdvisorLocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*Lesson03MemoryAdvisorOpenAiLiveTest'
```

앱을 [실습 README](../../../labs/08-00-advisors/README.md)의 명령으로 실행한 뒤, 같은 쿠키 파일로 두 요청을 보냅니다.<br>
앱 실행 후 요청에는 API 사용량이 발생합니다.<br>
예산 테스트를 처음부터 반복하려면 마지막의 삭제 요청을 먼저 실행합니다.

```bash
curl -sS -c /tmp/learning08-memory.cookies -b /tmp/learning08-memory.cookies \
  http://127.0.0.1:8098/api/advisors/memory/budget \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '선물 예산은 3만원입니다.'
curl -sS -c /tmp/learning08-memory.cookies -b /tmp/learning08-memory.cookies \
  http://127.0.0.1:8098/api/advisors/memory/budget \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '제가 말한 예산은 얼마인가요?'
curl -sS -X DELETE -b /tmp/learning08-memory.cookies \
  http://127.0.0.1:8098/api/advisors/memory/budget
```

| 테스트 | 확인하는 동작 |
|---|---|
| [Lesson03MemoryAdvisorLocalModelTest](../../../labs/08-00-advisors/src/test/java/com/example/ailearning/l08advisors/lesson03_memory/Lesson03MemoryAdvisorLocalModelTest.java) | 실제 두 번째 모델 요청의 메시지, 중복 저장 방지, 사용자·대화 격리, 삭제, 실패 시 질문 보존 |
| [Lesson03MemoryAdvisorOpenAiLiveTest](../../../labs/08-00-advisors/src/test/java/com/example/ailearning/l08advisors/lesson03_memory/Lesson03MemoryAdvisorOpenAiLiveTest.java) | 실제 두 번의 답변과 저장 개수, 후속 모델 요청에 이전 질문·실제 답변이 포함됨 |

먼저 LocalModel의 `후속_요청에_이전_대화를_전달하고_질문과_답변을_한번씩_저장한다`를 읽습니다.<br>
고정 응답을 쓰므로 전송한 메시지의 위치와 내용을 정확히 비교할 수 있습니다.

Live 테스트의 `@MockitoSpyBean ChatModel`은 실제 모델 호출을 그대로 실행하면서 호출 인자를 기록합니다.<br>
`ArgumentCaptor`는 Mockito에서 그 인자를 꺼내 검사하는 도구입니다.<br>
다음은 Live 테스트의 검증 부분 발췌입니다.

```java
var prompts = ArgumentCaptor.forClass(Prompt.class);
verify(chatModel, times(2)).call(prompts.capture());
var sentTexts = prompts.getAllValues().get(1).getInstructions().stream()
        .map(message -> message.getText())
        .toList();
assertThat(sentTexts).containsSubsequence(firstQuestion, firstReply.answer(), followUpQuestion);
```

`capture()`로 받은 두 Prompt 중 두 번째 것을 검사합니다.<br>
답변이 반드시 “3만원입니다”라는 문장인지 고정하지 않고, **이전 대화가 실제 후속 호출의 입력에 연결됐는지** 확인합니다.

## 정상 흐름 뒤에 확인할 차이: 실패한 질문의 저장 시점

Spring AI 2.1.0가 제공하는 `MessageChatMemoryAdvisor`는 모델 호출 전에 현재 사용자 질문을 저장하고, 정상 응답을 받은 뒤 모델의 답변 메시지를 저장합니다.<br>
이 클래스의 역할과 저장 시점은 [07-4의 첫 소개](../07-chat-memory/04-store.md#왜-이-실습은-모델-답변을-확인한-뒤-저장할까)에서 비교했습니다.<br>
이번에는 실제 Advisor 연결 상태에서 실패를 재현합니다.

| 구현 | 사용자 질문 저장 시점 | 모델 호출 실패 시 |
|---|---|---|
| 07단원의 `MemoryChatService` | 정상 답변을 확인한 뒤 질문·답변을 함께 저장 | 이번 질문·답변을 저장하지 않음 |
| `MessageChatMemoryAdvisor` | 모델 호출 전에 질문 저장, 정상 응답 후 답변 저장 | 이번 사용자 질문이 남을 수 있음 |

이 차이는 `모델_실패_후에도_기본_메모리_Advisor가_먼저_저장한_질문은_남는다` LocalModel 테스트에서 HTTP 503을 재현해 확인합니다.<br>
저장 기능을 `MessageChatMemoryAdvisor`에 맡기면 저장 시점도 그 클래스의 구현을 따릅니다.<br>
07단원의 성공 후 저장 정책까지 그대로 유지되는 것은 아닙니다.

## 확인 질문

1. 메모리 Advisor를 등록한 뒤에도 이전 메시지 조립과 저장을 서비스에서 다시 수행해야 하나요?
2. 같은 대화 ID를 사용하는 두 사용자의 대화를 구분하려면 무엇을 고려해야 하나요?
3. `.advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationKey))`에서 람다가 하는 일은 무엇인가요?
4. 모델 호출이 실패하면 현재 사용자 질문도 반드시 저장되지 않나요?

<details>
<summary>해설</summary>

1. 이 예제에서는 다시 수행하지 않습니다.<br>
   메모리 Advisor가 조회·전달·저장을 담당하므로 중복 처리될 수 있습니다.
2. 대화 ID만 같다고 같은 사용자는 아닙니다.<br>
   서버가 확인한 사용자 또는 세션과 대화 ID를 함께 사용해 저장 키를 구분해야 합니다.
3. 현재 요청의 대화 키를 설정합니다.<br>
   이미 등록한 메모리 Advisor가 그 값을 사용하며, 람다 자체는 새 Advisor가 아닙니다.
4. 아닙니다.<br>
   Spring AI 2.1.0의 `MessageChatMemoryAdvisor`는 호출 전에 사용자 질문을 저장하므로 실패한 질문이 남을 수 있습니다.<br>
   성공한 대화만 저장하는 요구와 맞는지 별도로 판단해야 합니다.

</details>

[이전: 등록 범위](02-registration.md) · [다음: 직접 구현과 순서](04-chain.md) · [단원 목차](../08-advisors.md)
