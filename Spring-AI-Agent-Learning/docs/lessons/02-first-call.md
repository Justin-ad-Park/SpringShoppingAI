# 02. 첫 모델 호출: 질문 하나와 문자열 응답

예상 시간: 2시간. [01장](01-setup.md)의 테스트를 먼저 통과시키세요.
목표는 세 클래스의 역할을 이해하고, 자신의 질문을 모델에 전달하는 것입니다.

## 만들 API

```text
POST /api/chat
Content-Type: text/plain
본문: Spring의 의존성 주입을 두 문장으로 설명해줘.

응답: 모델이 작성한 설명 문자열
```

아직 상품 데이터, 도구, 이전 대화, DB는 없습니다. 입력 문장을 전달하고 결과 문장을 받습니다.
JSON 요청 객체를 추가하기 전에 흐름에 집중하도록 요청과 응답을 단순한 텍스트로 구성했습니다.

## 1. 시작 클래스

파일: `Spring-AI-Agent-Learning/labs/01-first-chat/src/main/java/com/example/ailearning/FirstChatApplication.java`

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

일반 Spring Boot 애플리케이션의 시작 코드와 같습니다.
같은 패키지 아래의 Controller와 Service를 찾고, Starter의 자동 설정을 적용합니다.
새로운 AI 실행 엔진을 직접 만드는 과정은 없습니다.

## 2. HTTP 요청을 받는 Controller

파일: `Spring-AI-Agent-Learning/labs/01-first-chat/src/main/java/com/example/ailearning/ChatController.java`

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

생성자 주입으로 `ChatService`를 받습니다. `@RequestBody`는 HTTP 본문을 문자열로 읽습니다.
`consumes`는 받을 본문 형식, `produces`는 반환할 형식을 지정합니다.
`@NotBlank`는 공백뿐인 질문을, `@Size`는 2000자를 넘는 질문을 거부합니다.
최대 길이는 `MAX_MESSAGE_LENGTH` 상수 한 곳에서 정의합니다.
테스트도 `"가".repeat(ChatController.MAX_MESSAGE_LENGTH + 1)`로 같은 상수를 사용해,
허용 길이보다 정확히 한 글자 긴 입력을 보내면 HTTP 400이 반환되는지 확인합니다.
이 입력 검증은 Spring MVC/Validation의 기능이며 모델의 판단에 맡기지 않습니다.
이 실습의 2000자는 학습용 입력 제한입니다. 모델의 토큰 제한과 같은 값이 아닙니다.

## 3. 모델을 호출하는 Service

파일: `Spring-AI-Agent-Learning/labs/01-first-chat/src/main/java/com/example/ailearning/ChatService.java`

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
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
```

처음 보는 핵심은 `ChatClient` 하나입니다.
`ChatClient.Builder`는 Spring Boot 자동 설정이 준비하고 생성자로 주입합니다.
`build()`로 호출에 사용할 클라이언트를 만듭니다. `ask()`는 질문마다 새 요청을 구성합니다.

`prompt()` → 요청 준비, `user()` → 사용자 메시지 추가, `call()` → 동기 호출 지정,
`content()` → 호출 결과의 텍스트 꺼내기로 읽으세요.
이 연결을 평가하는 동안 외부 요청과 응답 대기가 일어납니다. Builder를 만들었다고 모델이 답변하는 것은 아닙니다.
세부 관계는 다음 장에서 배웁니다.

### 테스트 코드 읽기 (선택)

첫 실습의 필수 학습 범위는 `application.yml`의 연결 설정과 세 클래스의 역할,
`ChatService.ask()`의 네 줄입니다. 테스트는 제공된 검증 도구로 실행하면 충분하며,
가짜 HTTP 서버를 직접 구현하는 것은 이번 학습 목표에 포함하지 않습니다.

테스트도 읽고 싶다면 `FirstChatLocalMockModelTest.java`에서 다음 순서로 확인하세요.

1. `modelProperties()`: 테스트용 연결 설정
2. `sendsUserMessageToLocalMockModelAndReturnsFixedText()`: 질문이 전달되고 응답 문자열이 반환되는지 확인
3. `rejectsInvalidInputsBeforeCallingLocalMockModel()`: 입력 검증 보충. Spring MVC/Validation의 동작 확인

`doesNotSendPreviousRequestToLocalMockModelAutomatically()`는 03장의 대화 기억 실험에서 읽습니다.
인증 헤더 검증은 선택 심화이며, `ask()`와 `startModelServer()`는 제공된 보조 코드입니다.

로컬 MockModel 테스트는 `spring.config.import=`로 루트 `.env` 읽기를 끕니다.
`modelProperties()`의 설정은 `@DynamicPropertySource`를 통해 **테스트에서만** 적용됩니다.
`application.yml`의 같은 설정을 덮어쓰거나 테스트에 필요한 설정을 추가합니다.

| 설정 | 실제 실행 (`bootRun`) | 로컬 테스트 |
|---|---|---|
| `spring.ai.openai.api-key` | 루트 `.env`의 `OPENAI_API_KEY` | 더미 키 `local-test-key` |
| `spring.ai.openai.chat.options.model` | 루트 `.env`의 `OPENAI_MODEL` | 검증용 이름 `tutor-test-model` |
| `spring.ai.openai.base-url` | 별도로 지정하지 않은 제공 업체 연결 주소 | 테스트가 시작한 로컬 가짜 서버 주소 |

더미 키도 Spring AI의 연결 설정에 사용하므로 유지합니다. 가짜 서버는 이 키로 실제 계정 인증을 하지 않습니다.
테스트의 인증 헤더 검증은 `Bearer local-test-key`가 요청에 담겼는지 확인합니다.

테스트 중에는 두 개의 HTTP 서버가 동작합니다.

```text
테스트의 ask() → 애플리케이션 서버의 /api/chat → Controller → ChatService → Spring AI
  → 가짜 모델 서버의 /v1/chat/completions → 고정 JSON 응답 → 응답 텍스트 반환
```

`RANDOM_PORT`는 애플리케이션 서버에, `InetSocketAddress("127.0.0.1", 0)`은 가짜 모델 서버에
각각 사용 가능한 포트를 배정합니다. 테스트는 애플리케이션의 배정된 포트를 `@Value`로 받습니다.
가짜 서버의 고정 JSON은 Spring AI가 읽을 수 있는 OpenAI 응답 형식을 흉내 낸 것이며 AI가 생성한 답변은 아닙니다.

`@BeforeEach`는 테스트마다 요청 기록을 비우고, `@AfterAll`은 모든 테스트 후 가짜 서버를 종료합니다.
`CopyOnWriteArrayList`는 가짜 서버 처리 스레드와 테스트 스레드가 요청 기록을 공유하기 위해 사용합니다.
이 서버와 스레드 관련 구현은 첫 실습에서 넘어가도 됩니다.

## 4. 실제 모델 호출

01장에서 설명한 저장소 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`을 사용합니다.
저장소 루트에서 실행하세요.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:bootRun
```

시작 로그에서 애플리케이션이 8091 포트로 시작했는지 확인합니다.
새 터미널을 열고 다음 명령을 실행합니다. 이 터미널에는 API 키를 입력할 필요가 없습니다.
키는 Spring 애플리케이션이 외부 모델을 호출할 때 사용합니다.

```bash
curl -i http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw 'Spring의 의존성 주입을 초보자에게 두 문장으로 설명해줘.'
```

`--data-raw`가 본문을 보내므로 POST 요청이 됩니다. `-i`는 상태 코드와 헤더도 보여줍니다.
포트를 바꿨다면 curl 주소도 함께 바꿉니다.

다음은 **응답 형태 예시**이며 실제 모델 실행 결과를 복사한 것이 아닙니다.

```text
HTTP/1.1 200
Content-Type: text/plain;charset=UTF-8
...

의존성 주입은 객체가 필요한 다른 객체를 직접 만들지 않고 외부에서 전달받는 방식입니다.
Spring은 이 연결을 대신 관리해 코드의 결합도를 낮춰줍니다.
```

문장과 응답 시간은 모델·상황에 따라 달라집니다. 정확히 같은 문장이 나오는지가 성공 기준은 아닙니다.
상태 코드 200, 빈 문자열이 아닌 응답, 질문과 관련된 설명인지를 확인하세요.
200이어도 사실이 맞는지는 별도로 확인해야 합니다.

종료는 서버 터미널에서 Ctrl+C를 누릅니다.
실제 모델에 한 번 질문하고 응답만 출력해 보고 싶다면 서버 실행 대신 별도 테스트도 사용할 수 있습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:test --tests com.example.ailearning.FirstChatOpenAiLiveTest --rerun-tasks
```

`FirstChatOpenAiLiveTest`는 실제 OpenAI에 연결하므로 API 사용량이 발생합니다.
IntelliJ에서는 클래스 또는 메서드의 Run으로 실행합니다. 별도 허용 변수는 없습니다.
필터 없이 이 모듈의 `test`를 실행하면 두 클래스가 모두 실행됩니다.

## 5. 질문만 바꿔 보기

같은 curl 명령의 본문을 차례로 바꿔 봅니다.

1. “DI가 무엇인지 한 문장으로 설명해줘.”
2. “DI를 카페 주문에 비유해 설명해줘.”
3. “현재 우리 쇼핑몰의 재고를 알려줘.”

1·2에서는 요청 표현에 따라 설명 방식이 바뀌는지 확인합니다.
3에서는 이 코드가 쇼핑몰 API를 연결한 적이 없다는 점을 생각하세요.
모델이 재고 숫자를 말하더라도 실제 재고를 조회했다는 증거가 아닙니다. 이후 Tool Calling에서 해결할 문제입니다.

## 6. 입력 오류 실험

```bash
curl -i http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '   '
```

HTTP 400을 기대합니다. 모델 호출 전에 애플리케이션이 입력을 거부합니다.
이를 로컬 계약 테스트도 검증합니다. 오류 응답 본문의 세부 형식은 Boot 설정에 따라 달라질 수 있습니다.

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

첫 실습은 외부 오류를 업무용 응답으로 변환하는 예외 처리를 아직 추가하지 않았습니다.
따라서 제공 업체의 401 등이 클라이언트에는 일반 500으로 보일 수 있습니다. 서버 로그에서 원인을 확인합니다.
로그를 공유할 때는 키와 민감한 질문 내용을 제외하세요.
재시도·시간 제한·오류 응답 설계는 후속 평가·운영 수업에서 추가합니다.

## 확인 질문

1. 요청을 보내기 전에 모델을 학습시킨 적이 있나요?
2. Controller가 모델 제공 업체의 HTTP API를 직접 호출하나요?
3. 2000자 제한을 프롬프트로 지시하지 않고 Java 검증으로 둔 이유는 무엇인가요?

<details>
<summary>해설과 완료 기준</summary>

학습된 외부 모델을 사용하며 재학습하지 않습니다.
Controller는 Service를 호출하고, Service가 Spring AI를 통해 모델과 통신합니다.
입력 제한은 모델 요청 전에 확실하게 적용할 애플리케이션 규칙이기 때문입니다.
실제 키가 있다면 질문 하나를 보내고, 없다면 로컬 테스트 결과와 코드 경로를 설명한 뒤 다음 장으로 넘어가세요.
로컬 테스트만 수행했다면 실제 모델 호출까지 성공했다고 기록하지 마세요.
</details>

보충 읽기: [Chat Client API](https://docs.spring.io/spring-ai/reference/api/chatclient.html)의 첫 Builder 예제.

[이전: 01](01-setup.md) · [다음: 03. ChatClient와 프롬프트](03-chatclient-and-prompts.md)
