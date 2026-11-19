# 08-1. 기본 제공 Advisor로 요청·응답 로그 남기기

## 이번 수업의 목표

02장의 `ChatClient` 호출을 알고 있다면 시작할 수 있습니다. Advisor의 내부 구현이나 호출 순서는 아직 몰라도 됩니다.
기본 제공 기능 하나를 ChatClient에 연결하고 실제 요청·응답 로그를 확인합니다.

## 어떤 문제를 해결할까?

쇼핑 문의를 처리하다 보면 “모델에 어떤 질문을 보냈고 어떤 답변을 받았는가?”를 확인해야 합니다.
서비스마다 모델 호출 앞뒤에 로그 코드를 반복하는 대신, Spring AI의 `SimpleLoggerAdvisor`를 연결할 수 있습니다.
**Advisor는 ChatClient가 요청을 처리하는 과정에 연결하는 기능 객체**입니다. 이 로거는 질문의 답을 만드는 대신 요청·응답을 기록합니다.

이 수업은 기본 제공 객체를 사용하는 방법만 익힙니다. 메모리나 도구를 함께 연결할 필요는 없습니다.

## 기능을 연결하고 질문하기

아래는 [Lesson01LoggingChatService](../../../labs/08-00-advisors/src/main/java/com/example/ailearning/l08advisors/logging/Lesson01LoggingChatService.java)의 전체 코드입니다.
`SimpleLoggerAdvisor.builder().build()`는 Spring AI가 제공한 로거 객체를 만듭니다.
`defaultAdvisors(...)`는 그 로거를 이 ChatClient의 모든 요청에 적용하도록 등록합니다.

```java
package com.example.ailearning.l08advisors.logging;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Service;

/** 08-1: 기본 제공 Advisor 하나를 연결해 실제 모델 요청·응답 로그를 확인한다. */
@Service
public class Lesson01LoggingChatService {
    private final ChatClient chatClient;

    public Lesson01LoggingChatService(ChatClient.Builder builder) {
        chatClient = builder
                // 이 클라이언트로 만드는 모든 요청에 같은 기록 기능을 적용한다.
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
                .build();
    }

    public String chat(String question) {
        return chatClient
                .prompt()
                .user(question)
                .call()
                .content();
    }
}
```

1. 생성자에서 로거를 연결한 ChatClient를 한 번 만듭니다. 이때 모델에 질문하지는 않습니다.
2. `chat(question)`이 실행되면 그 질문으로 요청을 구성합니다.
3. `.call().content()`로 결과를 읽는 과정에서 로거가 요청을 기록하고 모델을 호출한 뒤 정상 응답을 기록합니다.
4. 반환하는 `String`은 모델 답변입니다. 로그는 반환 문자열에 붙지 않고 서버 콘솔에 출력됩니다.

`default`가 붙는 이유와 요청 하나에만 연결하는 방법은 다음 수업에서 비교합니다.

## 로그를 보려면 DEBUG 설정도 필요하다

이 로거는 DEBUG 수준에서 출력합니다. 모듈의 [application.yml](../../../labs/08-00-advisors/src/main/resources/application.yml)에 다음 설정이 준비되어 있습니다.

```yaml
logging:
  level:
    org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor: DEBUG
```

로거를 등록해도 로그 수준이 INFO라면 본문 로그가 보이지 않습니다.
학습용 질문으로 확인하세요. 실제 고객 대화를 기록할 때는 원문에 포함된 개인정보를 고려해 기록 범위를 정해야 합니다.

## 실행과 테스트

저장소 루트에서 실행합니다. LocalModel은 고정 응답 HTTP 서버를 사용하며 외부 API를 호출하지 않습니다.
Live는 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`로 실제 OpenAI를 호출하므로 API 사용량이 발생합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*Lesson01LoggingLocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*Lesson01LoggingOpenAiLiveTest'
```

- [Lesson01LoggingLocalModelTest](../../../labs/08-00-advisors/src/test/java/com/example/ailearning/l08advisors/logging/Lesson01LoggingLocalModelTest.java): HTTP 응답과 로거가 출력한 `request:`, `response:`를 확인합니다.
- [Lesson01LoggingOpenAiLiveTest](../../../labs/08-00-advisors/src/test/java/com/example/ailearning/l08advisors/logging/Lesson01LoggingOpenAiLiveTest.java): 실제 모델과 연결해 이 수업의 기능을 확인합니다. 자연어 답변 문구를 고정하지 않습니다.


[실습 README](../../../labs/08-00-advisors/README.md)의 명령으로 앱을 실행한 뒤 다음 요청도 사용할 수 있습니다. 실제 API 사용량이 발생합니다.

```bash
curl -sS http://127.0.0.1:8098/api/advisors/logging \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '상품 문의를 하려면 어떤 정보를 준비해야 하나요?'
```

콘솔의 `SimpleLoggerAdvisor` 행에서 `request:`와 `response:`를 찾습니다. 다음은 전체 로그에서 타임스탬프·부가 필드를 생략한 형태 예시입니다.

```text
SimpleLoggerAdvisor : request: ...질문이 포함된 요청...
SimpleLoggerAdvisor : response: ...모델 응답...
```

## 확인 질문

1. 서비스의 `chat()` 안에 로그 코드가 없어도 요청과 응답이 기록되는 이유는 무엇인가요?
2. 로거를 등록했는데 로그가 보이지 않으면 어떤 설정을 확인해야 하나요?

<details>
<summary>해설</summary>

1. ChatClient를 만들 때 로거 Advisor를 연결했기 때문입니다. 해당 클라이언트의 요청 처리 과정에서 Spring AI가 그 기능을 실행합니다.
2. `SimpleLoggerAdvisor`의 로그 수준이 DEBUG인지 확인합니다. 정상 응답 로그는 모델 호출이 정상 반환한 뒤 출력됩니다.

</details>

참고: [Spring AI ChatClient — 로깅](https://docs.spring.io/spring-ai/reference/api/chatclient.html).

[단원 목차](../08-advisors.md) · [다음: 기본 등록과 요청별 등록](02-registration.md)
