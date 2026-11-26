# 02. 첫 모델 호출: 질문 하나와 문자열 응답

## 한 번에 읽을 범위

아래 세 주제를 순서대로 학습합니다.<br>
첫 학습에서는 세 클래스와 호출 결과에 집중하고, 테스트용 HTTP 서버 구현은 제공된 검증 도구로 사용합니다.

| 순서 | 읽고 해볼 것 | 여기서 확인할 것 |
|---|---|---|
| 1 | Controller와 Service의 한 요청 | 입력 문자열이 ask()로 전달되는 위치 찾기 |
| 2 | ChatService.ask의 네 줄 | 요청 조립과 응답 획득을 구분 |
| 3 | 로컬 테스트와 입력 검증 | 정상 응답과 잘못된 입력의 HTTP 400 확인. API 키가 있으면 실제 응답도 비교 |

[공통 학습 방법](../learning-guide.md)

[01단원의 API 키 없는 테스트](01-setup.md#3-api-키-없이-테스트-실행)를 먼저 실행하세요.<br>
테스트의 정상 응답과 HTTP 400을 보고, `Controller`에서 `ChatService`를 거쳐 모델 요청이 나가는 경로를 코드로 설명할 수 있으면 이 단원의 기본 실습을 마친 것입니다.<br>
API 키가 있다면 [실제 모델 호출](#4-실제-모델-호출)에서 질문을 보내고 답변을 비교하세요.<br>
생성자 주입의 내부 동작, 테스트 코드 읽기, 다중 모델은 선택 보충입니다.

## 만들 API

```text
POST /api/chat
Content-Type: text/plain
본문: Spring의 의존성 주입을 두 문장으로 설명해줘.

응답: 모델이 작성한 설명 문자열
```

아직 상품 데이터, 도구, 이전 대화, DB는 없습니다.<br>
입력 문장을 전달하고 결과 문장을 받습니다.<br>
JSON 요청 객체를 추가하기 전에 흐름에 집중하도록 요청과 응답을 단순한 텍스트로 구성했습니다.

## 1. 시작 클래스

파일: `Spring-AI-Agent-Learning/labs/02-00-first-chat/src/main/java/com/example/ailearning/FirstChatApplication.java`

```java
package com.example.ailearning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FirstChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(FirstChatApplication.class, args);
    }
}
```

일반 Spring Boot 애플리케이션의 시작 코드와 같습니다.<br>
같은 패키지 아래의 Controller와 Service를 찾고, Starter의 자동 설정을 적용합니다.

## 2. HTTP 요청을 받는 Controller

파일: `Spring-AI-Agent-Learning/labs/02-00-first-chat/src/main/java/com/example/ailearning/ChatController.java`

```java
package com.example.ailearning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    public static final int MAX_MESSAGE_LENGTH = 2000;

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/api/chat", consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String chat(@RequestBody @NotBlank @Size(max = MAX_MESSAGE_LENGTH) String message) {
        return chatService.ask(message);
    }
}
```

생성자 주입으로 `ChatService`를 받습니다.<br>
`@RequestBody`는 HTTP 본문을 문자열로 읽습니다.<br>
`consumes`는 받을 본문 형식, `produces`는 반환할 형식을 지정합니다.<br>
`@NotBlank`는 공백뿐인 질문을, `@Size`는 2000자를 넘는 질문을 거부합니다.<br>
최대 길이는 `MAX_MESSAGE_LENGTH` 상수 한 곳에서 정의합니다.<br>
이 입력 검증은 Spring MVC/Validation의 기능이며 모델의 판단에 맡기지 않습니다.<br>
이 실습의 2000자는 학습용 입력 제한입니다.<br>
모델의 토큰 제한과 같은 값이 아닙니다.

## 3. 모델을 호출하는 Service

파일: `Spring-AI-Agent-Learning/labs/02-00-first-chat/src/main/java/com/example/ailearning/ChatService.java`

```java
package com.example.ailearning;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String ask(String message) {
        return chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }
}
```

처음 보는 핵심은 `ChatClient` 하나입니다.<br>
`ChatClient.Builder`는 Spring Boot 자동 설정이 준비하고 생성자로 주입합니다.<br>
OpenAI용 객체가 선택되고 설정값이 적용되는 과정은 [생성자 주입 상세 설명](02-first-call/01-constructor-injection.md)에서 따로 살펴봅니다.<br>
프롬프트의 메시지와 역할별 호출 코드는 [프롬프트·메시지·역할 이해하기](00-llm-and-spring-ai/01-prompt-and-messages.md)에서 보충 학습할 수 있습니다.

`build()`로 호출에 사용할 클라이언트를 만듭니다.<br>
`ask()`는 질문마다 새 요청을 구성합니다.

`prompt()`는 요청 구성을 시작하고, `user()`는 사용자 메시지를 설정합니다.<br>
**`.call().content()`로 동기 요청을 실행하고 응답 텍스트를 받습니다.**
`call()`은 동기 응답을 얻는 방식을 선택하며, `content()`로 결과를 얻을 때 실제 요청을 수행하고 응답을 기다립니다.<br>
Builder를 만들거나 메시지를 준비하는 것만으로 모델이 호출되지는 않습니다.<br>
세부 관계는 다음 단원에서 배웁니다.

### 테스트 코드 읽기 (선택)

테스트 설정과 두 HTTP 서버의 역할, 선택해서 읽을 메서드는 [02-2. 테스트 코드 읽기](02-first-call/02-test-code.md)에 정리했습니다.

## 4. 실제 모델 호출

API 키를 준비했다면 실제 모델에 질문을 보내 봅니다.<br>
서버 실행·curl 요청·실제 OpenAI 테스트 실행과 결과 해석은 [02-3. 실제 모델 호출](02-first-call/03-live-model-call.md)을 따라가세요.<br>
실행 후 아래의 질문 변경·입력 오류 실험으로 이어집니다.

## 5. 질문만 바꿔 보기

같은 curl 명령의 본문을 차례로 바꿔 봅니다.

1. “DI가 무엇인지 한 문장으로 설명해줘.”
2. “DI를 카페 주문에 비유해 설명해줘.”
3. “현재 우리 쇼핑몰의 재고를 알려줘.”

1·2에서는 요청 표현에 따라 설명 방식이 바뀌는지 확인합니다.<br>
3에서는 이 코드가 쇼핑몰 API를 연결한 적이 없다는 점을 생각하세요.<br>
모델이 재고 숫자를 말하더라도 실제 재고를 조회했다는 증거가 아닙니다.<br>
이후 Tool Calling에서 해결할 문제입니다.

## 6. 입력 오류 실험

```bash
curl -i http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '   '
```

HTTP 400을 기대합니다.<br>
모델 호출 전에 애플리케이션이 입력을 거부합니다.<br>
이를 로컬 계약 테스트도 검증합니다.<br>
오류 응답 본문의 세부 형식은 Boot 설정에 따라 달라질 수 있습니다.

## 7. 외부 호출에 실패하면

| 현상 | 의미와 확인 순서 |
|---|---|
| 서버가 시작되지 않음 | Java, 환경 변수, 포트부터 확인. 아직 모델 호출 문제가 아닐 수 있음 |
| 클라이언트 응답 415 | 요청의 Content-Type이 text/plain인지 확인 |
| 클라이언트 응답 400 | 빈 본문, 공백, 2000자 초과 확인 |
| 서버 로그에 제공 업체 401 | 키가 없거나 유효하지 않은지 확인 |
| 제공 업체 403/404 | 모델 이름·접근 권한·제공 여부 확인 |
| 제공 업체 429 | 호출 제한·계정 사용량·한도 확인 |
| 통신 오류·타임아웃 | 네트워크, 프록시, 외부 서비스 상태 확인 |

첫 실습은 외부 오류를 업무용 응답으로 변환하는 예외 처리를 아직 추가하지 않았습니다.<br>
따라서 제공 업체의 401 등이 클라이언트에는 일반 500으로 보일 수 있습니다.<br>
서버 로그에서 원인을 확인합니다.<br>
로그를 공유할 때는 키와 민감한 질문 내용을 제외하세요.<br>
[첫 실습의 `application.yml`](../../labs/02-00-first-chat/src/main/resources/application.yml)에서 OpenAI SDK의 요청 시간 제한은 `spring.ai.openai.timeout: 5s`, 자동 재시도는 0회입니다.<br>
[Live 테스트](../../labs/02-00-first-chat/src/test/java/com/example/ailearning/FirstChatOpenAiLiveTest.java)의 `@Timeout(60)`은 JUnit이 테스트 메서드 전체에 적용하는 60초 제한입니다.<br>
지연·재시도는 첫 호출을 익힌 뒤 [02 보충 5: 모델 요청 타임아웃과 재시도](02-first-call/05-model-timeout-retry.md)에서 고정 응답으로 확인할 수 있습니다.<br>
후속 평가·운영 수업에서는 전체 실행 시간 제한과 오류 응답 설계를 더 자세히 배웁니다.

## 확인 질문

1. 요청을 보내기 전에 모델을 학습시킨 적이 있나요?
2. Controller가 모델 제공 업체의 HTTP API를 직접 호출하나요?
3. 2000자 제한을 프롬프트로 지시하지 않고 Java 검증으로 둔 이유는 무엇인가요?

<details>
<summary>해설</summary>

1. 아닙니다.<br>
   이미 학습된 외부 모델을 사용하며 이 예제에서 재학습하지 않습니다.
2. 아닙니다.<br>
   Controller는 Service를 호출하고, Service가 Spring AI를 통해 모델과 통신합니다.
3. 입력 제한은 모델 요청 전에 확실하게 적용해야 하는 애플리케이션 규칙이기 때문입니다.

</details>

실제 키가 있다면 질문 하나를 보내고, 없다면 로컬 테스트 결과와 코드 경로를 설명한 뒤 다음 단원으로 넘어가세요.<br>
로컬 테스트만 수행했다면 실제 모델 호출까지 성공했다고 기록하지 마세요.

보충 읽기: [Chat Client API](https://docs.spring.io/spring-ai/reference/api/chatclient.html)의 첫 Builder 예제.

[이전: 01](01-setup.md) · [다음: 03. ChatClient와 프롬프트](03-chatclient-and-prompts.md)
