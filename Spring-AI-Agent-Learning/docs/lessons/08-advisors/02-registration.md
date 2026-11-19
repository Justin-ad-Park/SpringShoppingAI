# 08-2. 모든 요청의 기본 설정과 이번 요청의 추가 설정

## 이번 수업의 목표

[08-1](01-built-in.md)의 로거를 사용해 `defaultAdvisors(...)`와 요청 단계의 `advisors(...)`가 적용되는 범위를 구분합니다.
내부 구현이나 실행 순서보다 “어느 요청에 이 기능이 필요한가?”를 먼저 판단합니다.

## default는 무엇의 기본일까?

`defaultAdvisors(...)`는 **이 빌더로 만드는 ChatClient가 사용할 기본 Advisor 목록**입니다.
그 ChatClient로 새 질문을 보낼 때마다 기본 목록이 적용되므로 매번 같은 Advisor를 전달할 필요가 없습니다.
Java의 `default` 키워드나 애플리케이션 전체에 적용되는 전역 설정이라는 뜻은 아닙니다.

요청을 만드는 `.prompt()` 다음의 `.advisors(Advisor...)`는 **이번 요청의 목록에 Advisor를 추가**합니다.
기본 목록을 교체하지 않으며, 다음 `.prompt()`로 만드는 요청에는 그 추가가 남지 않습니다.

| 설정 위치 | 적용 범위 | 적합한 상황 |
|---|---|---|
| `builder.defaultAdvisors(...)` | 생성한 ChatClient의 모든 요청 | 상담 클라이언트에서 항상 메모리 사용 |
| `chatClient.prompt().advisors(advisor)` | 지금 구성하는 요청만 | 특정 요청에 추가 진단 기능 사용 |

생성자에 있느냐, 실행 중 호출하느냐보다 **빌더의 기본 설정인지, 현재 요청의 설정인지**가 기준입니다.
예를 들어 실행 중 새 클라이언트를 만드는 코드에서도 `defaultAdvisors`를 사용하면 그 새 클라이언트의 기본 설정입니다.

“기본 제공 Advisor”와 “기본 등록 Advisor”도 구분하세요. 전자는 Spring AI가 구현한 기능이고, 후자는 내가 모든 요청에 적용하도록 선택한 기능입니다. 직접 구현한 Advisor도 기본 등록할 수 있습니다.

## 같은 로거로 범위만 비교하기

다음은 [Lesson02AdvisorRegistrationService](../../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/registration/Lesson02AdvisorRegistrationService.java) 전체 코드입니다.
이 수업에는 메모리·도구·커스텀 Advisor를 넣지 않습니다.

```java
package com.example.ailearning.l08advisors.registration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.stereotype.Service;

/** 08-2: 기본 등록과 이번 요청에만 추가하는 등록의 범위만 비교한다. */
@Service
public class Lesson02AdvisorRegistrationService {
    private final ChatClient chatClient;

    public Lesson02AdvisorRegistrationService(ChatClient.Builder builder) {
        chatClient = builder
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
                .build();
    }

    public String chatWithDefaults(String question) {
        return chatClient
                .prompt()
                .user(question)
                .call()
                .content();
    }

    public String chatWithAdditionalAdvisor(String question, Advisor additionalAdvisor) {
        return chatClient
                .prompt()
                .user(question)
                // 기본 Advisor를 교체하지 않는다. 이번 요청에만 추가한다.
                .advisors(additionalAdvisor)
                .call()
                .content();
    }
}
```

아래는 이 서비스를 주입받은 테스트에서 실행할 호출 순서의 발췌입니다. `service`의 준비와 검증문은 아래 테스트 파일에 있습니다.
추가 기능을 새로 배우지 않고 등록 범위만 확인하려고 같은 로거를 두 개 사용합니다. 평소 서비스에서 같은 로그를 두 번 남기라는 권장 예시는 아닙니다.

```java
service.chatWithAdditionalAdvisor("추가 로그가 필요한 질문", SimpleLoggerAdvisor.builder().build());
service.chatWithDefaults("일반 질문");
```

| 호출 | 실행되는 로거 | `request:` 로그 수 | 모델 요청 수 |
|---|---|---:|---:|
| 첫 호출 | 기본 로거 + 이번 요청의 추가 로거 | 2 | 1 |
| 다음 호출 | 기본 로거만 | 1 | 1 |

Advisor가 두 개여도 같은 한 번의 모델 호출을 감쌀 수 있습니다. 로거 수와 모델 요청 수를 같다고 보지 않습니다.
두 로거의 order는 같으므로 둘 사이의 순서는 여기서 검증하지 않습니다. 순서를 지정하는 방법은 [08-4](04-chain.md)에서 다룹니다.

## Spring AI가 적용하는 방식

Spring AI 2.0.2의 `DefaultChatClient` 구현에서 `.prompt()`는 기본 요청 설정을 복사해 새 요청을 만듭니다.
그 새 요청의 `advisors(Advisor...)`는 목록에 객체를 더합니다. 그래서 기본 기능은 유지되고, 요청별 추가는 그 요청에만 남습니다.
IntelliJ에서 `ChatClient` 구현인 `DefaultChatClient.prompt()`와 `DefaultChatClientRequestSpec.advisors(Advisor...)`를 열면 이 동작을 확인할 수 있습니다.

## 실행과 테스트

저장소 루트에서 실행합니다. LocalModel은 고정 응답 HTTP 서버를 사용하며 외부 API를 호출하지 않습니다.
Live는 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`로 실제 OpenAI를 호출하므로 API 사용량이 발생합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*Lesson02AdvisorRegistrationLocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*Lesson02AdvisorRegistrationOpenAiLiveTest'
```

- [Lesson02AdvisorRegistrationLocalModelTest](../../../labs/08-00-advisors/src/test/java/com/example/ailearning/l08advisors/registration/Lesson02AdvisorRegistrationLocalModelTest.java): 기본 로거의 반복 실행, 요청별 로거 추가, 다음 요청에서 추가 로거 미실행을 로그 수와 Spy로 검사합니다.
- [Lesson02AdvisorRegistrationOpenAiLiveTest](../../../labs/08-00-advisors/src/test/java/com/example/ailearning/l08advisors/registration/Lesson02AdvisorRegistrationOpenAiLiveTest.java): 실제 모델과 연결해 이 수업의 기능을 확인합니다. 자연어 답변 문구를 고정하지 않습니다.


이 수업은 Service를 직접 주입받아 실행합니다. Advisor는 개발자가 등록하는 Java 객체이므로 추가 로거를 HTTP 요청으로 받는 API는 만들지 않습니다.
로거 설정은 08-1과 같은 application.yml의 DEBUG 설정을 사용합니다.

## 같은 advisors인데 람다를 받는 코드는 무엇일까?

`advisors`에는 받는 인자가 다른 메서드들이 있습니다. Java에서는 이를 오버로딩이라고 합니다.

| 호출 모양 | 실제 인자 타입 | 하는 일 |
|---|---|---|
| `.advisors(additionalAdvisor)` | `Advisor...` | 현재 요청에 실행할 Advisor 객체 추가 |
| `.advisors(spec -> spec.param(key, value))` | `Consumer<ChatClient.AdvisorSpec>` | 현재 요청의 Advisor 설정을 작성 |

두 번째는 Advisor 구현체를 전달하는 코드가 아닙니다. 이 람다는 설정용 객체를 받아 값을 채우는 함수입니다.
`param()`은 이미 등록된 Advisor가 읽을 값을 지정하며, 그 자체로 새 Advisor를 등록하지 않습니다.
설정 함수 안의 `spec.advisors(...)`로 객체를 추가할 수도 있지만, 다음 수업에서는 `param()`으로 대화 ID만 지정합니다.

## 확인 질문

1. 기본 로거를 등록한 클라이언트에 요청별 로거를 하나 더 추가하면 기본 로거는 사라지나요?
2. 그다음 요청에서 `.advisors(...)`를 생략하면 어떤 로거가 적용되나요?
3. `.advisors(spec -> spec.param(...))`가 새 Advisor를 등록한다고 볼 수 있나요?

<details>
<summary>해설</summary>

1. 사라지지 않습니다. 기본 로거에 현재 요청의 로거가 추가됩니다.
2. 기본 로거만 적용됩니다. 이전 요청에서 추가한 로거는 다음 요청에 남지 않습니다.
3. 이 코드에서는 아닙니다. `param()`은 현재 요청의 설정값을 채웁니다. Advisor 객체를 추가하는 호출과 인자 타입·역할이 다릅니다.

</details>

참고: [Spring AI ChatClient API](https://docs.spring.io/spring-ai/reference/api/chatclient.html). 등록 범위는 사용 버전 2.0.2의 `DefaultChatClient` 소스와 위 테스트로 함께 확인합니다.

[이전: 기본 로거](01-built-in.md) · [다음: 대화 메모리와 파라미터](03-memory.md) · [단원 목차](../08-advisors.md)
