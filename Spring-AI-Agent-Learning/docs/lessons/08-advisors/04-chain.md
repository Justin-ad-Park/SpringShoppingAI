# 08-4. Advisor 직접 구현과 전후 처리 순서

## 이번 수업의 목표

[08-1](01-built-in.md)에서 기능 사용법, [08-2](02-registration.md)에서 등록 범위, [08-3](03-memory.md)에서 요청별 파라미터를 배웠습니다.
이번에는 제공되는 기능으로 해결하지 못하는 전후 처리를 직접 구현하고, 기본·요청별 Advisor가 합쳐졌을 때 실행 순서를 예측합니다.

## CallAdvisor는 다음 처리를 감싼다

`CallAdvisor`는 `.call()` 방식의 동기 요청에 참여하는 인터페이스입니다.
`adviseCall(request, chain)`에서 `chain.nextCall(request)`를 호출하면 다음 Advisor로 요청을 넘기고 그 결과가 돌아올 때까지 기다립니다.
그 앞에 요청 전 처리를, 반환 뒤에 응답 후 처리를 넣습니다. 스트리밍용 인터페이스는 `StreamAdvisor`이며 이번 실습에서는 사용하지 않습니다.

`TraceAdvisor`는 이 실습에서 직접 구현하는 Advisor입니다. 답변을 바꾸지 않고 `DefaultA:before` 같은 실행 단계만 남깁니다.
다음은 [TraceAdvisor](../../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/chain/TraceAdvisor.java) 전체 코드입니다. `record`는 생성자로 받은 이름·순서를 보관하는 Java 타입이고, Advisor 동작은 `adviseCall()`에 구현합니다.

```java
package com.example.ailearning.l08advisors.chain;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import com.example.ailearning.l08advisors.chain.support.AdvisorTrace;

/** 08-4에서 전후 처리 순서를 기록하고, 08-5에서 전체 요청과 모델 요청의 범위를 비교한다. */
public record TraceAdvisor(String advisorName, int advisorOrder) implements CallAdvisor {
    public static final String TRACE_CONTEXT_KEY = "lesson.advisor.trace";

    @Override
    public String getName() {
        return advisorName;
    }

    @Override
    public int getOrder() {
        return advisorOrder;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // TraceAdvisor는 이름·순서만 보관하고, 실행 기록은 현재 요청의 context에서 받는다.
        var advisorTrace = (AdvisorTrace) request.context().get(TRACE_CONTEXT_KEY);
        advisorTrace.record(advisorName + ":before");
        try {
            // 다음 Advisor 또는 모델을 호출하고, 반환될 때까지 기다린다.
            return chain.nextCall(request);
        } finally {
            // 정상 응답과 실패 모두에서 종료를 기록한다. 성공했다는 표시가 아니다.
            advisorTrace.record(advisorName + ":after");
        }
    }
}
```

`finally`는 예외가 나도 실행됩니다. 이 예제의 `after`는 종료 기록이며 정상 답변을 받았다는 보장은 아닙니다.
08-1의 기본 로거가 출력하는 `response:`와는 의미가 다릅니다.

## 기본 두 개 + 이번 요청 두 개 등록하기

다음은 [Lesson04AdvisorChainChatService](../../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/chain/Lesson04AdvisorChainChatService.java) 전체 코드입니다.
이름이 `Default`로 시작한다고 자동으로 기본 설정이 되는 것은 아닙니다. `defaultAdvisors(...)`에 넣었기 때문에 모든 요청에 적용됩니다.

```java
package com.example.ailearning.l08advisors.chain;

import com.example.ailearning.l08advisors.chain.support.AdvisorTrace;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/** 08-4: 기본 등록과 요청별 등록을 합친 뒤 order에 따라 전후 처리를 실행한다. */
@Service
public class Lesson04AdvisorChainChatService {
    public record Reply(String answer, List<String> steps) {}

    private final ChatClient chatClient;

    public Lesson04AdvisorChainChatService(ChatClient.Builder builder) {
        chatClient = builder
                // 이 ChatClient의 모든 요청에 적용한다.
                .defaultAdvisors(new TraceAdvisor("DefaultA", 0), new TraceAdvisor("DefaultB", 10))
                .build();
    }

    public Reply chat(String question) {
        var advisorTrace = new AdvisorTrace();
        String answer = chatClient
                .prompt()
                .user(question)
                // AdvisorA/B는 이번 요청에만 추가한다. 모든 요청에 적용하는 DefaultA/B도 함께 실행된다.
                .advisors(new TraceAdvisor("AdvisorA", 20), new TraceAdvisor("AdvisorB", 30))
                .advisors(advisorSpec -> advisorSpec.param(TraceAdvisor.TRACE_CONTEXT_KEY, advisorTrace))
                .call()
                .content();
        return new Reply(answer, advisorTrace.steps());
    }

    /** 다음 요청에 AdvisorA/B가 남지 않는지 비교하는 기본 등록 전용 호출. */
    public Reply chatWithDefaults(String question) {
        var advisorTrace = new AdvisorTrace();
        String answer = chatClient
                .prompt()
                .user(question)
                .advisors(advisorSpec -> advisorSpec.param(TraceAdvisor.TRACE_CONTEXT_KEY, advisorTrace))
                .call()
                .content();
        return new Reply(answer, advisorTrace.steps());
    }
}
```

| Advisor 이름 | 등록 위치 | getOrder() |
|---|---|---:|
| DefaultA | defaultAdvisors | 0 |
| DefaultB | defaultAdvisors | 10 |
| AdvisorA | 이번 요청의 advisors | 20 |
| AdvisorB | 이번 요청의 advisors | 30 |

Spring AI는 두 목록을 합친 뒤 `getOrder()` 값이 작은 Advisor부터 실행합니다.
이 숫자에서는 요청 전 처리가 DefaultA → DefaultB → AdvisorA → AdvisorB이고, 응답 후 처리는 반대입니다.
각 Advisor가 다음 처리의 반환을 기다리는 구조이기 때문입니다.

```text
DefaultA:before
  DefaultB:before
    AdvisorA:before
      AdvisorB:before
        모델 요청 → 응답
      AdvisorB:after
    AdvisorA:after
  DefaultB:after
DefaultA:after
```

**모든 요청에 적용하도록 등록한 Advisor가 언제나 먼저 실행되는 것은 아닙니다.** 예를 들어 이번 요청에만 추가한 AdvisorA의 order를 -10으로 바꾸면 DefaultA·DefaultB보다 먼저 진입하고 마지막에 종료합니다.
order가 같으면 순서를 보장하지 않으므로, 순서가 중요할 때는 예제처럼 다른 값을 지정합니다.
기준: [Spring AI Advisor Order](https://docs.spring.io/spring-ai/reference/api/advisors.html#_advisor_order).

## 요청마다 다른 기록을 어디에 둘까?

기록이 섞이는 원인은 `defaultAdvisors(...)` 자체가 아니라 **여러 요청이 같은 기록 객체를 변경하는 것**입니다.
멤버 변수가 있다는 사실만으로 문제가 생기는 것도 아닙니다. 어떤 값을 보관하고 그 값이 요청 처리 중 바뀌는지 구분해야 합니다.

| 보관하는 값 | 현재 `TraceAdvisor`의 구성 | 요청 사이에 공유해도 되는가? |
|---|---|---|
| Advisor 이름·실행 순서 | record의 `advisorName`, `advisorOrder`. 생성 후 바뀌지 않는 설정 | 공유 가능 |
| 요청마다 달라지는 실행 기록 | `TraceAdvisor`에는 기록 목록을 보관하지 않음 | 같은 변경 가능한 목록을 여러 요청이 사용하면 기록이 섞임 |

예를 들어 `TraceAdvisor`에 `final List<String> steps`를 두고 모든 요청이 그 목록에 기록하면, 순차 요청에서도 이전 기록이 쌓이고 동시 요청에서는 기록이 끼어들 수 있습니다. `final`은 목록 참조를 다른 목록으로 바꾸지 못하게 할 뿐, 목록 내용의 변경을 막지 않습니다.
이 문제는 하나의 객체를 여러 요청에서 재사용할 때 생기며, 같은 객체를 매 요청의 `.advisors(...)`로 전달하는 경우에도 발생할 수 있습니다.

현재 `Lesson04AdvisorChainChatService`는 요청마다 새 `AdvisorTrace`를 만들고, 08-3에서 배운 `param(...)`으로 전달합니다.
`TraceAdvisor`는 `request.context()`에서 **현재 요청의 기록 객체**를 읽습니다. 같은 객체를 재사용해도 이름·순서만 공유하고 요청 기록은 공유하지 않는 구성입니다.
단, context에 넣는다고 객체가 복사되는 것은 아닙니다. 요청마다 `new AdvisorTrace()`를 만드는 코드가 기록을 분리합니다. 이 context 값 자체가 모델의 질문으로 전송되는 것은 아닙니다.

다음은 보조 클래스 [AdvisorTrace](../../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/chain/support/AdvisorTrace.java) 전체 코드입니다. `steps()`는 현재 기록을 복사해 응답으로 반환합니다.

```java
package com.example.ailearning.l08advisors.chain.support;

import java.util.ArrayList;
import java.util.List;

/** 한 요청에서 실행한 Advisor의 전후 처리 기록. 요청마다 새 객체를 만든다. */
public class AdvisorTrace {
    private final List<String> steps = new ArrayList<>();

    public void record(String step) {
        steps.add(step);
    }

    public List<String> steps() {
        return List.copyOf(steps);
    }
}
```

## 실행과 테스트

저장소 루트에서 실행합니다. LocalModel은 고정 응답 HTTP 서버를 사용하며 외부 API를 호출하지 않습니다.
Live는 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`로 실제 OpenAI를 호출하므로 API 사용량이 발생합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*Lesson04AdvisorChainLocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*Lesson04AdvisorChainOpenAiLiveTest'
```

- [Lesson04AdvisorChainLocalModelTest](../../../labs/08-00-advisors/src/test/java/com/example/ailearning/l08advisors/chain/Lesson04AdvisorChainLocalModelTest.java): 네 Advisor의 정확한 전후 순서, 요청별 기록 분리, 다음 기본 호출에서 추가 Advisor 미실행, 공백 질문 차단을 검사합니다.
- [Lesson04AdvisorChainOpenAiLiveTest](../../../labs/08-00-advisors/src/test/java/com/example/ailearning/l08advisors/chain/Lesson04AdvisorChainOpenAiLiveTest.java): 실제 모델과 연결해 이 수업의 기능을 확인합니다. 자연어 답변 문구를 고정하지 않습니다.


[실습 README](../../../labs/08-00-advisors/README.md)의 명령으로 앱을 시작한 뒤 다음 요청을 보낼 수 있습니다. 실제 모델 사용량이 발생합니다.

```bash
curl -sS http://127.0.0.1:8098/api/advisors/chain \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '안녕하세요.'
```

응답의 `steps`에는 위의 before 네 개와 after 네 개가 들어갑니다. `answer`는 모델마다 다릅니다.
테스트 `요청에_추가한_Advisor는_다음_기본_호출에_남지_않는다`는 같은 서비스의 `chat()` 다음에 `chatWithDefaults()`를 실행합니다.
두 번째 요청의 결과는 다음과 같습니다.

```text
[DefaultA:before, DefaultB:before, DefaultB:after, DefaultA:after]
```

## 확인 질문

1. 위 표의 네 Advisor는 전처리와 후처리에서 각각 어떤 순서로 실행되나요?
2. AdvisorA의 order만 -10으로 바꾸면 `defaultAdvisors(...)`에 등록한 DefaultA·DefaultB가 먼저 실행되나요?
3. 하나의 `TraceAdvisor` 객체에 모든 요청의 기록 목록을 보관하지 않고, 요청마다 새 `AdvisorTrace`를 만들어 전달하는 이유는 무엇인가요?

<details>
<summary>해설</summary>

1. 전처리는 DefaultA → DefaultB → AdvisorA → AdvisorB, 후처리는 AdvisorB → AdvisorA → DefaultB → DefaultA입니다. 순서 값이 작은 쪽이 먼저 진입하고 나중에 반환합니다.
2. 아닙니다. 두 등록 목록을 합쳐 order로 정렬하므로 AdvisorA가 먼저 진입합니다. `defaultAdvisors(...)`와 요청 단계의 `.advisors(...)` 중 어디에 등록하는지는 적용할 요청의 범위를 결정합니다.
3. 같은 `TraceAdvisor` 객체가 요청별로 바뀌는 기록 목록까지 공유하면 다른 요청의 기록이 섞일 수 있습니다. 현재 구현은 `TraceAdvisor`에 기록 목록을 보관하지 않고, 요청마다 만든 `AdvisorTrace`를 그 요청의 context로 전달합니다.

</details>

[이전: 메모리와 파라미터](03-memory.md) · [다음: 도구 루프](05-tool-loop.md) · [단원 목차](../08-advisors.md)
