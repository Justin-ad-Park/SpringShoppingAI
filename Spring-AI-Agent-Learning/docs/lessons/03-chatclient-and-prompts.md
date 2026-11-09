# 03. ChatClient, 메시지, 프롬프트 이해하기

## 한 번에 읽을 범위

아래 세 주제를 순서대로 읽고 실습합니다. 00장 보충에서 익힌 프롬프트 구성을 02장의 실행 코드에 적용합니다.
테스트 서버의 내부 구현은 직접 작성하지 않아도 됩니다.

| 순서 | 읽고 해볼 것 | 여기서 확인할 것 |
|---|---|---|
| 1 | ChatClient·ChatModel·Builder | 클라이언트 생성 위치와 요청에서 재사용하는 위치 찾기 |
| 2 | 서비스에 응답 원칙 추가 | 실행 중인 서버에서 system 추가 전후를 비교 |
| 3 | 기억 없는 두 요청 | 두 번째 요청에 이전 메시지가 없음을 확인 |

[공통 학습 방법](../learning-guide.md)

02장의 코드와 같은 실습 프로젝트를 사용합니다.
새 라이브러리를 추가하지 않고 메시지 한 줄만 바꿉니다.

선수 내용: [00장 보충의 역할 개요](00-llm-and-spring-ai/01-prompt-and-messages.md#spring-ai에서-구분하는-네-가지-역할)와 [예시 1·2](00-llm-and-spring-ai/01-prompt-and-messages.md#예시-1-사용자-메시지-하나로-구성한-프롬프트).
메시지 역할, `.prompt().user(...)`·`.system(...)`, `.call().content()`의 기본 구성은 이 보충 문서를 기준으로 읽습니다.
이번 단원은 **클라이언트를 어디에서 만들고 재사용하는지, 코드 변경이 실제 요청에 어떻게 반영되는지**를 확인합니다.

## 1. ChatClient와 ChatModel

| 구분 | 역할 | 지금 직접 작성하는가? |
|---|---|---|
| ChatClient | 요청과 응답 처리를 편하게 구성하는 상위 API | Service에서 사용 |
| ChatModel | 채팅 모델 호출을 표현하는 인터페이스 | Starter가 구현 객체를 준비 |
| OpenAiChatModel: OpenAI용 ChatModel 구현 | 제공 업체의 API와 연결 | Starter가 설정을 바탕으로 구성 |
| 실제 모델 | 입력을 처리해 답변 생성 | 외부 제공 업체에서 실행 |

현재 코드는 `ChatClient.Builder`를 주입받아 `ChatClient`를 만듭니다. 실제 모델이 Java 객체 안에 들어오는 것은 아닙니다.
객체는 외부 모델을 호출하는 연결을 사용합니다.

## 2. Builder는 왜 필요할까?

**Builder**는 여러 설정을 모아 객체를 만드는 방식입니다.
Spring은 설정에 맞는 `ChatClient.Builder`를 준비해 Service 생성자에 전달합니다.

다음은 [ChatService.java](../../labs/02-00-first-chat/src/main/java/com/example/ailearning/ChatService.java)에서 필드·생성자·호출 메서드를 함께 발췌한 코드입니다. package·import·`@Service`는 생략했습니다.

```java
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

- 생성자의 `builder.build()`가 클라이언트를 만들고 `chatClient` 필드에 보관합니다.
- `ask()`는 호출될 때마다 이 필드를 사용합니다. 요청마다 새 ChatClient를 만드는 코드는 없습니다.
- `ask()`에는 이전 질문·답변을 저장하거나 다음 요청에 추가하는 코드가 없습니다. 실제 전송 메시지는 4절의 로컬 테스트로 확인합니다.
- Builder를 직접 `new`로 만들지 않은 이유와 OpenAI 연결이 준비되는 과정은 [02장 생성자 주입 설명](02-first-call/01-constructor-injection.md)에서 다시 확인할 수 있습니다. 클라이언트 재사용과 대화 기억은 별개의 기능입니다.

## 3. 실험: 서비스에 응답 원칙 추가하기

실제 모델을 호출하는 실험입니다. 먼저 [01장의 루트 .env 설정](01-setup.md#4-실제-모델-연결을-위한-설정-준비)을 준비하세요. 호출에는 비용이 발생할 수 있습니다.
키가 없다면 실행을 생략하고 코드와 아래 로컬 Mock 테스트로 메시지 구성을 확인할 수 있습니다.

먼저 원본 코드를 실행하고 다른 터미널에서 수정 전 응답을 저장합니다. 이미 서버가 실행 중이면 중복 실행하지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-00-first-chat:bootRun
```

```bash
curl -sS http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw 'DI가 뭐야?' > /tmp/learning03-before.txt
cat /tmp/learning03-before.txt
```

이제 2절에서 읽은 `ChatService.ask()`의 `chatClient.prompt()` 다음, `.user(message)` 앞에 다음 한 줄을 추가합니다.
아래는 **추가할 줄만 발췌**한 코드입니다. package·import·생성자·Controller·설정 파일은 02장 그대로 둡니다.

```java
.system("Spring 입문자를 가르치는 튜터입니다. 쉬운 한국어로 세 문장 이내로 설명하세요.")
```

기본 구성은 [00장 보충의 예시 2](00-llm-and-spring-ai/01-prompt-and-messages.md#예시-2-여러-메시지로-구성한-프롬프트)와 같습니다.
여기서는 이미 실행한 서비스에 지시를 추가하고, 같은 사용자 질문의 응답이 어떻게 달라지는지 비교합니다.

기존 서버를 Ctrl+C로 종료하고 같은 터미널에서 다시 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-00-first-chat:bootRun
```

다른 터미널에서 같은 질문을 보냅니다.

```bash
curl -i http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw 'DI가 뭐야?'
```

추가 전후 응답을 비교하세요. 길이·단어 난이도·예시가 달라지는지 관찰합니다.
답변이 세 문장을 넘으면 “코드 오류인가, 모델이 지시를 따르지 않은 것인가?”를 나눠 생각합니다.
사용자 질문은 같지만 system 메시지가 추가되어 전체 프롬프트는 달라집니다.
교재의 문장 수 지시는 실험용이며 자동 검증이나 보장을 제공하지 않습니다.

실험 후 system 줄을 제거해 원본으로 되돌리세요.
제공된 로컬 Mock 테스트는 원본의 user 메시지 하나를 검증합니다. system을 추가한 상태라면 메시지 개수 검사가 실패하므로, 원본으로 복원한 뒤 실행합니다.
되돌린 후 확인합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-00-first-chat:test --tests com.example.ailearning.FirstChatLocalModelTest
```

## 4. 실험: 이전 대화를 보내지 않는 요청

앞 실험의 서버를 Ctrl+C로 종료하고, system 줄을 제거한 원본 코드로 다시 시작합니다. 소스만 수정해도 이미 실행 중인 서버에 자동 반영되는 것은 아닙니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-00-first-chat:bootRun
```

다른 터미널에서 다음 두 요청을 차례로 보냅니다.

```bash
curl -sS http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '제 이름은 민수입니다.'

curl -sS http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '제 이름이 무엇인가요?'
```

[00장 보충의 두 단계 대화 예제](00-llm-and-spring-ai/01-prompt-and-messages.md#단순-문장인-assistant-메시지를-chatclient에-전달하기)는 첫 질문·답변을 두 번째 프롬프트에 직접 넣었습니다.
지금 실행한 `ask()`는 매 요청의 `message`만 전달합니다. 두 구현에서 **이전 메시지를 넣는 코드가 있는지** 비교하세요.
모델이 우연히 이름을 맞히는지는 기억 여부의 확실한 증거가 아닙니다.
[FirstChatLocalModelTest](../../labs/02-00-first-chat/src/test/java/com/example/ailearning/FirstChatLocalModelTest.java)의 `다음_요청에_이전_대화를_자동으로_포함하지_않는다` 테스트는 두 번째 외부 요청에 현재 질문만 들어갔는지 확인합니다.
[07장 대화 메모리](07-chat-memory.md)에서는 이전 대화를 여러 HTTP 요청에 걸쳐 저장·조회하는 구성을 실습합니다.

## 5. 이번 실습에서 프롬프트와 코드의 책임

| 이번 서비스에서 확인할 요구 | 담당하는 위치 | 확인할 점 |
|---|---|---|
| 쉬운 한국어로 세 문장 이내 설명 | 추가한 system 지시 | 실제 응답을 비교. 문장 수를 코드로 검사한 것은 아님 |
| 공백 질문 거부 | 기존 Controller의 입력 검증 | system을 추가하거나 제거해도 입력 검증 코드는 그대로임 |
| 이전 대화 재전달 | 애플리케이션의 메시지 구성 코드 | 현재 ask()에는 없으며, 클라이언트 재사용만으로 추가되지 않음 |

system을 수정하는 작업과 Java의 입력 검증·메시지 저장 기능을 구현하는 작업을 구분하세요.

## 확인 질문

1. `ChatClient.Builder`를 생성자로 받는 이유는 무엇인가요?
2. user 메시지와 system 메시지는 각각 누구의 어떤 내용을 담나요?
3. system 메시지에 “가격을 틀리지 마라”라고 쓰면 가격 검증 코드가 필요 없을까요?
4. 같은 ChatClient 객체를 재사용하면 이전 질문도 기억하나요?

<details>
<summary>해설</summary>

1. 자동 설정이 준비한 Builder로 모델 연결이 구성된 ChatClient를 만들기 위해서입니다.
2. user는 사용자의 요청, system은 애플리케이션의 역할·응답 원칙 등을 담습니다.
3. 필요합니다. 프롬프트 지시는 정확성 보장이 아니므로 실제 데이터와 Java 검증 코드가 필요합니다.
4. 아닙니다. ChatClient 재사용만으로 대화 기록이 저장·전달되지는 않습니다.

</details>

코드 네 줄을 설명하고 메시지 추가 실험과 기억 실험의 의미를 설명하면 이번 묶음을 마친 것입니다.

보충 읽기: [Chat Client API](https://docs.spring.io/spring-ai/reference/api/chatclient.html)의 프롬프트 구성과 Builder 부분.

[이전: 02](02-first-call.md) · [다음: 04. 구조화 응답](04-structured-output.md) · [학습 지도](../../README.md)
