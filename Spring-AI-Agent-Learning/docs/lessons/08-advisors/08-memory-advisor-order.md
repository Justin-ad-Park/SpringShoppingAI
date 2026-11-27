# 08-8. 메모리 Advisor의 order와 저장 메시지 비교하기

**이번 목표:** 메모리 Advisor의 기본 order가 정해지는 과정을 이해하고, 도구 반복 앞뒤 배치에 따라 실행 횟수와 저장되는 메시지가 어떻게 달라지는지 확인합니다.<br>
[08-3의 대화 메모리](03-memory.md)와 [08-7의 도구 반복과 order](07-tool-loop-order.md)를 배운 뒤 읽습니다.

## 비교할 세 객체

이 수업에서는 “허브티 가격은 얼마인가요?”라는 질문으로 가격 도구를 한 번 실행한 뒤 최종 답변을 받습니다.<br>
같은 흐름에서 메모리 Advisor의 order만 바꾸어 비교합니다.

| 객체 | 역할 |
|---|---|
| `MessageChatMemoryAdvisor` | 이전 대화를 프롬프트에 추가하고, 현재 처리한 메시지를 메모리에 저장 |
| `MessageWindowChatMemory` | 실제 대화 메시지를 보관·조회하는 이 실습의 메모리 구현. 변수 타입은 `ChatMemory` |
| `ToolCallingAdvisor` | 모델의 도구 요청을 처리하고, 도구 결과로 모델에 다시 요청하는 반복 담당 |

`before()`·`after()`는 메모리 객체가 아니라 **메모리 Advisor의 메서드**입니다.<br>
테스트는 실행이 끝난 뒤 `conversationMemory.get(conversationId)`로 실제 저장된 메시지를 꺼내 출력합니다.

## 1. order를 생략하면 누가 값을 정할까?

```java
var memoryAdvisor = MessageChatMemoryAdvisor.builder(conversationMemory)
        .build();
```

Spring AI 2.1.0에서 `MessageChatMemoryAdvisor.Builder`의 order 필드는 `DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER`로 초기화됩니다.<br>
아래는 실제 구현에서 order 관련 부분만 발췌한 코드이며, 다른 필드와 생성자는 생략했습니다.

```java
public static final class Builder {
    private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;

    public Builder order(int order) {
        this.order = order;
        return this;
    }

    public MessageChatMemoryAdvisor build() {
        return new MessageChatMemoryAdvisor(this.chatMemory, this.order, this.scheduler);
    }
}
```

이 값은 `Advisor` 인터페이스에 다음과 같이 정의되어 있습니다.

```java
int DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER = Ordered.HIGHEST_PRECEDENCE + 200;
```

`.order(...)`를 호출하지 않으면 이 값이 그대로 유지됩니다.<br>
`build()`가 생성자 인자로 전달하고, 생성자는 `this.order`에 보관합니다.<br>
이후 체인을 정렬할 때 `getOrder()`가 이 값을 반환합니다.<br>
Spring의 의존성 주입이나 `defaultAdvisors(...)`가 order를 정하는 것이 아닙니다.

| 대상                                                   | order                                             |                 비교 | 처리기준                     |
|------------------------------------------------------|---------------------------------------------------|-------------------:|--------------------------|
| ToolCallingAdvisor 기준값                               | `DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 300` |                 기준 |                          |
| MessageChatMemoryAdvisor 기본값                         | `Ordered.HIGHEST_PRECEDENCE + 200`                | 기준 보다 작음<br> -100 | out of loop |
| MessageChatMemoryAdvisor에서<br>toolCalling 메시지를 남기려면 | 예)`Ordered.HIGHEST_PRECEDENCE + 400`              |  기준 보다 큼<br> +100 | in the loop |


`Ordered.HIGHEST_PRECEDENCE`는 Java int의 가장 작은 값입니다.<br>
음수인지 양수인지보다 **ToolCallingAdvisor의 order보다 작은지 큰지**가 배치를 결정합니다.<br>

## 2. 기본 order: 사용자 질문과 최종 답변 저장

다음은 [로컬 테스트](../../../labs/08-00-advisors/src/test/java/com/example/ailearning/l08advisors/lesson08_memoryorder/Lesson08MemoryOrderLocalModelTest.java)의 기본값 비교 부분입니다.<br>
`spy(...)`는 실제 메모리 Advisor를 그대로 실행하면서 메서드 호출을 관찰합니다.<br>
`chatModel`은 Spring Boot가 준비하고, 메모리와 가격 도구는 테스트마다 새 객체로 준비합니다.

```java
var memoryAdvisor = spy(MessageChatMemoryAdvisor.builder(conversationMemory)
        .build());
var service = new Lesson08MemoryOrderChatService(chatModel, memoryAdvisor, teaPriceTools);

String answer = service.chat(CONVERSATION_ID, "허브티 가격은 얼마인가요?");

var calls = MemoryOrderTestOutput.printAdvisorCalls(memoryAdvisor);
var messages = conversationMemory.get(CONVERSATION_ID);
MemoryOrderTestOutput.printMessages(messages);
```

메모리 Advisor의 order가 ToolCallingAdvisor보다 작으므로 다음 순서로 실행됩니다.

```text
MessageChatMemoryAdvisor.before(): 사용자 질문 저장
  ToolCallingAdvisor 처리 시작
    모델 요청 1: 질문 + 사용 가능한 도구 정보
    모델 응답 1: getTeaPrice 실행 요청
    Java 가격 도구 실행 → 12000
    모델 요청 2: 질문 + 도구 호출 요청 + 도구 실행 결과
    모델 응답 2: 허브티 가격은 12000원입니다.
  ToolCallingAdvisor가 최종 응답 반환
MessageChatMemoryAdvisor.after(): 최종 assistant 답변 저장
```

모델을 두 번 요청해도 메모리 Advisor의 before·after는 각각 한 번만 실행됩니다.<br>
ToolCallingAdvisor는 자기 뒤의 체인만 다시 호출하므로, 앞에 있는 메모리 Advisor는 반복 실행되지 않습니다.

**첫 번째 모델 응답은 ToolCallingAdvisor가 받아 도구를 실행하는 데 사용하며, 이 시점에는 바깥의 메모리 Advisor로 반환하지 않습니다.**<br>
도구 결과로 모델에 다시 요청해 최종 답변을 받은 뒤에야 메모리 Advisor로 반환합니다.<br>
그래서 기본 배치의 `after()`는 중간 도구 요청이 아닌 최종 답변을 전달받습니다.

로컬 테스트에서 확인한 출력입니다.

```text
메모리 Advisor 실행 순서: [before, after]
ChatMemory에 저장된 메시지:
1. role=USER, text=허브티 가격은 얼마인가요?
2. role=ASSISTANT, text=허브티 가격은 12000원입니다.
```

### 두 번째 모델 요청과 최종 응답은 다르다

도구 호출 내역을 모델에 보냈다는 사실과, 그 내역이 최종 응답에도 들어 있다는 것은 다릅니다.

| 구분 | 이 실습에서 담긴 내용 |
|---|---|
| 두 번째 모델 요청의 프롬프트 | 시스템 지침·사용자 질문·assistant의 도구 호출 요청·tool의 실행 결과 |
| 최종 모델 응답의 assistant 메시지 | “허브티 가격은 12000원입니다.”라는 답변. `toolCalls`는 비어 있음 |
| 기본 배치에서 저장된 대화 메모리 | `before()`가 저장한 사용자 질문 + `after()`가 저장한 최종 assistant 답변 |

`MessageChatMemoryAdvisor.after()`는 **모델에 보낸 프롬프트 전체를 저장하지 않습니다.**<br>
반환받은 `ChatResponse`에서 모델이 출력한 assistant 메시지를 꺼내 저장합니다.<br>
따라서 두 번째 요청에 도구 호출 요청과 결과가 포함되어 있어도, 그것들이 메모리에 함께 저장되는 것은 아닙니다.

도구 메시지까지 저장하려면 메모리 Advisor가 중간 모델 응답과 다음 모델 요청에도 참여해야 합니다.<br>
다음 비교에서는 order를 바꾸어 그 차이를 확인합니다.

## 3. 더 큰 order: 도구 호출 요청과 결과도 저장

두 번째 테스트에서는 다음 설정을 사용하고 나머지 서비스·도구는 그대로 둡니다.

```java
var memoryAdvisor = spy(MessageChatMemoryAdvisor.builder(conversationMemory)
        .order(ToolCallingAdvisor.DEFAULT_ORDER + 100)
        .build());
var service = new Lesson08MemoryOrderChatService(chatModel, memoryAdvisor, teaPriceTools);
```

메모리 Advisor가 ToolCallingAdvisor 뒤에 배치되어 모델을 요청할 때마다 실행됩니다.<br>
이 샘플의 `ChatClient`는 `ToolCallingAdvisor`를 자동 등록할 때 뒤쪽의 메모리 Advisor를 확인합니다.<br>
메모리 Advisor가 뒤에 있으면 이전 메시지를 그 Advisor가 붙이도록 설정합니다.<br>
두 번째 요청을 만들 때 `ToolCallingAdvisor`가 시스템 지침과 방금 받은 도구 결과를 넘기면, `MessageChatMemoryAdvisor.before()`가 메모리에 있던 사용자 질문과 도구 호출 요청을 합칩니다.<br>
[Spring AI 2.1.0의 자동 등록 코드](https://github.com/spring-projects/spring-ai/blob/v2.1.0/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/DefaultChatClient.java#L1271)와 [다음 요청에 넘길 메시지를 고르는 코드](https://github.com/spring-projects/spring-ai/blob/v2.1.0/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/ToolCallingAdvisor.java#L250)에서 이 연결을 확인할 수 있습니다.

도구를 한 번 실행하는 이 로컬 테스트에서는 모델 요청이 두 번이므로 다음 순서로 동작합니다.

| 시점 | 메모리 Advisor 동작 | 이번에 저장하는 메시지 |
|---|---|---|
| 모델 요청 1 전 | `before()` | 사용자 질문 |
| 모델 응답 1 후 | `after()` | 도구 호출 요청이 포함된 assistant 메시지 |
| Java 도구 실행 후, 모델 요청 2 전 | `before()` | 도구 실행 결과인 tool 메시지 |
| 모델 응답 2 후 | `after()` | 최종 답변인 assistant 메시지 |

실제로 메모리에 저장된 메시지를 출력하면 다음과 같습니다.

```text
메모리 Advisor 실행 순서: [before, after, before, after]
ChatMemory에 저장된 메시지:
1. role=USER, text=허브티 가격은 얼마인가요?
2. role=ASSISTANT, text=
   도구 요청: id=price-1, name=getTeaPrice, arguments={"productName":"허브티"}
3. role=TOOL, text=
   도구 결과: id=price-1, name=getTeaPrice, result=12000
4. role=ASSISTANT, text=허브티 가격은 12000원입니다.
```

두 번째 assistant 메시지는 문장 대신 도구 호출 정보를 담고 있어 본문이 비어 있을 수 있습니다.<br>
따라서 출력 코드는 본문뿐 아니라 도구 호출 ID·이름·인자와 결과까지 보여 줍니다.<br>
도구 요청과 결과의 ID가 같은지도 테스트에서 검증합니다.

실제 모델이 도구를 여러 번 요청하면 실행 횟수와 저장되는 도구 메시지도 늘어날 수 있습니다.<br>
반대로 도구 없이 바로 답하면 두 배치 모두 before·after가 각각 한 번 실행됩니다.<br>
여기서 2회·4개라는 수치는 도구 요청 한 번과 최종 답변으로 응답을 고정한 로컬 테스트의 결과입니다.

## 4. 어떤 조건에서 어떤 메시지를 저장할까?

Spring AI 2.1.0의 `MessageChatMemoryAdvisor.before()`는 이번 요청의 **마지막 사용자 메시지 또는 도구 결과 메시지**를 선택해 저장합니다.<br>
`after()`는 다음 처리가 정상 반환한 모델 응답의 assistant 메시지를 저장합니다.<br>
프롬프트의 모든 메시지를 통째로 저장하는 방식은 아닙니다.

| 메시지 | 기본 배치에서 저장 | 반복 안 배치에서 저장 | 이유 |
|---|---|---|---|
| system: 공통 답변 지침 | X | X | `before()`의 저장 대상이 아니며 모델 응답도 아님 |
| user: 현재 사용자 질문 | O | O | 첫 `before()`가 선택 |
| assistant: 도구 호출 요청 | X | O | 반복 안에서는 첫 모델 응답도 `after()`가 전달받음 |
| tool: 도구 실행 결과 | X | O | 반복 안에서는 다음 모델 요청 전 `before()`가 선택 |
| assistant: 최종 답변 | O | O | 마지막 정상 모델 응답을 `after()`가 저장 |

**system 메시지는 저장하지 않아도 모델에 전달됩니다.**<br>
서비스의 `.defaultSystem(...)`이 각 요청의 프롬프트에 준비하며, 메모리에서 가져온 이전 대화와 함께 모델에 전달됩니다.<br>
로컬 테스트는 실제 전송된 첫 메시지가 `system`인지 확인하고, 저장된 역할 목록에는 `SYSTEM`이 없음을 검사합니다.

이것은 메모리 Advisor의 저장 대상 선택에 관한 설명입니다.<br>
`MessageWindowChatMemory` 자체가 system 메시지를 저장할 수 없다는 뜻은 아닙니다.<br>
직접 메모리에 추가하는 경우와 구분해야 합니다.

`before()`의 저장은 모델 요청 전에 실행됩니다.<br>
따라서 이후 모델 호출이 실패해도 이미 저장한 사용자 질문은 남을 수 있고, 정상 반환하지 않으면 그 호출의 `after()`는 실행되지 않습니다.<br>
또한 이 수업에서는 메모리 창을 충분히 크게 설정했습니다.<br>
오래된 메시지 제거는 order와 별개인 `MessageWindowChatMemory`의 보관 정책입니다.

## 5. 실행 코드와 테스트

기존 실습과 같은 `08-00-advisors` 모듈 안의 **새 `lesson08_memoryorder` 패키지**만 사용합니다.<br>
이 샘플의 서비스·도구는 테스트에서 직접 생성하며 기존 앱의 빈으로 등록하지 않습니다.

| 읽을 코드 | 역할 |
|---|---|
| [Lesson08MemoryOrderChatService](../../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/lesson08_memoryorder/Lesson08MemoryOrderChatService.java) | 준비한 메모리 Advisor를 defaultAdvisors로 등록하고 도구를 사용한 대화 실행 |
| [TeaPriceTools](../../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/lesson08_memoryorder/TeaPriceTools.java) | 상품명을 받아 고정 가격 12000원을 반환하는 학습용 도구 |
| [Lesson08MemoryOrderLocalModelTest](../../../labs/08-00-advisors/src/test/java/com/example/ailearning/l08advisors/lesson08_memoryorder/Lesson08MemoryOrderLocalModelTest.java) | 두 배치의 실행 순서·저장 메시지·system 전달과 두 번째 요청의 도구 결과를 고정 응답으로 검증 |
| [Lesson08MemoryOrderOpenAiLiveTest](../../../labs/08-00-advisors/src/test/java/com/example/ailearning/l08advisors/lesson08_memoryorder/Lesson08MemoryOrderOpenAiLiveTest.java) | 실제 OpenAI로 배치·후속 질문·6개 메시지 창을 비교하고 메모리 내용 출력 |
| [MemoryOrderTestOutput](../../../labs/08-00-advisors/src/test/java/com/example/ailearning/l08advisors/lesson08_memoryorder/MemoryOrderTestOutput.java) | Spy의 before·after 호출 기록과 메모리 메시지를 출력하는 테스트 보조 코드 |

저장소 루트에서 다음 명령을 실행합니다.<br>
실제 API를 호출하지 않으며 키도 필요 없습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*Lesson08MemoryOrderLocalModelTest'
```

실제 OpenAI 비교는 다음 명령으로 실행합니다.<br>
루트 `.env`의 `OPENAI_API_KEY`·`OPENAI_MODEL`이 필요하며 API 사용량이 발생합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*Lesson08MemoryOrderOpenAiLiveTest'
```

Live 테스트는 실제 가격 도구가 실행됐는지 확인합니다.<br>
기본 배치는 전후 처리 한 번과 user·assistant 두 메시지를 검증하고, 반복 안 배치는 모델이 도구를 다시 요청할 가능성을 고려해 반복 횟수를 정확히 두 번으로 고정하지 않습니다.

추가 Live 테스트는 같은 상품을 다시 묻는 경우와 다른 상품을 묻는 경우를 실행해 저장된 내용을 출력합니다.<br>
다른 상품 비교에서는 `maxMessages(6)`을 사용해 도구 반복의 저장 대상 선택과 메시지 창의 오래된 대화 제거를 함께 확인합니다.<br>
6은 최대 보관 수이며, 오래된 대화를 정리한 뒤에도 항상 메시지 6개를 채워 두는 설정은 아닙니다.<br>
`TeaPriceTools`는 상품명을 인자로 받지만, 이 실습에서는 상품 구분 없이 12000원을 반환합니다. 실제 상품 가격 조회나 검색 정확도를 검증하는 예제는 아닙니다.

## 확인 질문

1. 기본 order에서 모델을 두 번 요청했는데 저장된 메시지가 두 개인 이유는 무엇인가요?
2. 반복 안에 배치하면 system 메시지도 저장되나요?
3. 도구 결과가 메모리에 없다는 것이 최종 답변을 만들 때 모델에도 전달되지 않았다는 뜻인가요?

<details>
<summary>해설</summary>

1. 메모리 Advisor가 도구 반복 앞에 있어 사용자 질문 처리 전과 최종 결과 반환 후에만 실행되기 때문입니다.<br>
   중간 도구 요청과 결과는 이 Advisor의 저장 대상에 들어오지 않습니다.
2. 아닙니다.<br>
   배치는 실행 시점을 바꾸지만, `before()`가 마지막 user·tool 메시지를 저장하고 `after()`가 assistant 메시지를 저장한다는 선택 방식은 같습니다.
3. 아닙니다.<br>
   기본 배치에서도 ToolCallingAdvisor가 현재 질문의 도구 결과를 다음 모델 요청에 전달합니다.<br>
   다음 사용자 질문에 재사용할 메모리에 저장하는 것과는 다릅니다.

</details>

## 참고 자료

- [Spring AI 공식 문서: Memory and the Tool Loop](https://docs.spring.io/spring-ai/reference/api/tools.html#memory-and-the-tool-loop): 기본 배치와 반복 안 배치, 도구 메시지를 저장할 수 있는 저장소 설명.
- [Spring AI 공식 문서: Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html): 메모리와 저장소, 대화 ID와 메시지 보관 정책.
- [Spring AI 2.1.0: MessageChatMemoryAdvisor 구현](https://github.com/spring-projects/spring-ai/blob/v2.1.0/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/MessageChatMemoryAdvisor.java): Builder의 order 초기화·생성자 전달과 `before()`·`after()`의 저장 대상.
- [Spring AI 2.1.0: Advisor의 기본 order](https://github.com/spring-projects/spring-ai/blob/v2.1.0/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/api/Advisor.java): `DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER` 정의.

공식 웹 문서는 현재 버전의 내용으로 갱신되므로, 이 실습의 동작은 2.1.0 소스와 로컬 테스트를 기준으로 확인합니다.<br>
반복 안 배치에는 도구 메시지를 저장할 수 있는 저장소가 필요하며, 이 실습의 기본 인메모리 저장소는 이를 지원합니다.<br>

[08-7](07-tool-loop-order.md)은 로거의 출력으로 **체인이 어느 구간에서 다시 실행되는지** 확인합니다.<br>
이 수업의 [로컬 테스트](../../../labs/08-00-advisors/src/test/java/com/example/ailearning/l08advisors/lesson08_memoryorder/Lesson08MemoryOrderLocalModelTest.java)는 여기에 더해 **메모리에 남은 메시지의 역할과 내용**을 확인합니다.

[이전: 도구 반복과 Advisor 배치](07-tool-loop-order.md) · [08단원 목차](../08-advisors.md)
