# 03. ChatClient, 메시지, 프롬프트 이해하기

## 한 번에 읽을 범위

각 행을 20~30분씩 읽고 관찰합니다. 첫 읽기에서는 Service의 핵심 호출에 집중하고,
테스트 HTTP 서버·직렬화 보조 코드는 제공된 검증 도구로 사용합니다.

| 순서 | 읽고 해볼 것 | 여기서 확인할 것 |
|---|---|---|
| 1 | ChatClient·ChatModel·Builder | Builder를 만드는 것과 호출하는 것의 차이 설명 |
| 2 | 프롬프트의 메시지 역할 | system 한 줄 추가 전후를 비교 |
| 3 | 기억 없는 두 요청 | 두 번째 요청에 이전 메시지가 없음을 확인 |

[공통 학습 방법](../learning-guide.md)

예상 시간: 2시간. 02장의 코드와 같은 실습 프로젝트를 사용합니다.
새 라이브러리를 추가하지 않고 메시지 한 줄만 바꿉니다.

## 1. ChatClient와 ChatModel

| 구분 | 역할 | 지금 직접 작성하는가? |
|---|---|---|
| ChatClient | 요청과 응답 처리를 편하게 구성하는 상위 API | Service에서 사용 |
| ChatModel | 채팅 모델 호출을 표현하는 인터페이스 | Starter가 구현 객체를 준비 |
| OpenAI용 ChatModel 구현 | 제공 업체의 API와 연결 | Starter가 설정을 바탕으로 구성 |
| 실제 모델 | 입력을 처리해 답변 생성 | 외부 제공 업체에서 실행 |

`ChatClient`를 주입받는다고 실제 모델이 Java 객체 안에 들어오는 것은 아닙니다.
객체는 외부 모델을 호출하는 연결을 사용합니다.

## 2. Builder는 왜 필요할까?

**Builder**는 여러 설정을 모아 객체를 만드는 방식입니다.
Spring은 설정에 맞는 `ChatClient.Builder`를 준비해 Service 생성자에 전달합니다.

```java
public ChatService(ChatClient.Builder builder) {
    this.chatClient = builder.build();
}
```

여기서 클라이언트 하나를 만들고 요청마다 재사용합니다.
이 필드에 이전 사용자의 질문을 저장하는 코드는 없습니다. 클라이언트 재사용과 대화 기억은 다른 문제입니다.
첫 수업에서 Builder를 직접 `new`로 만들거나 제공 업체 SDK를 따로 설정하지 않은 이유는 Starter의 자동 설정을 사용하기 때문입니다.

## 3. 호출을 한 줄씩 읽기

```java
return chatClient.prompt()
        .user(message)
        .call()
        .content();
```

| 코드 | 의미 | 오해하지 말 것 |
|---|---|---|
| `prompt()` | 이번 요청을 구성하기 시작 | 빈 요청 준비만으로 답변이 생성되지 않음 |
| `user(message)` | 사용자 역할의 메시지 추가 | 이 문자열이 시스템 규칙이나 DB 데이터가 되는 것은 아님 |
| `call()` | 동기 응답을 얻는 호출 방식 선택 | 전체 연결의 결과를 얻는 동안 응답을 기다림 |
| `content()` | 응답의 텍스트를 얻음 | 텍스트를 얻었다고 사실 검증이 끝난 것은 아님 |

여러 메서드를 점으로 이어 쓰는 방식을 fluent API라고 부릅니다.
지금은 위 네 줄을 읽을 수 있으면 됩니다. 스트리밍과 비동기 API는 아직 사용하지 않습니다.

## 4. 프롬프트와 메시지의 관계

프롬프트는 이번 모델 호출에 보낼 메시지 등을 담는 요청입니다.
각 메시지에는 역할이 있습니다.

| 역할 | 용도 | 예 |
|---|---|---|
| system | 애플리케이션이 지정하는 역할·응답 원칙 | “초보자에게 쉬운 한국어로 설명하라” |
| user | 사용자가 요청한 내용 | “DI가 뭐야?” |
| assistant | 모델이 앞서 작성한 응답 | 이전 대화를 함께 보낼 때 사용 |
| tool | 애플리케이션이 실행한 도구의 결과 | 상품 검색 결과. 이후 수업에서 사용 |

메시지를 나누면 지시의 목적을 명확하게 표현할 수 있습니다.
system 메시지는 애플리케이션 코드처럼 절대적으로 강제되는 보안 규칙은 아닙니다.
모델이 지시를 따르지 않을 가능성이 있으므로 금액·권한·입력 제한은 코드에서도 검증합니다.

## 5. 한 줄 추가 실험: 답변 방식 지정

`ChatService.java`의 **ask 메서드만** 다음 코드로 잠시 바꿉니다.
package·import·생성자·Controller·설정 파일은 02장 그대로 둡니다.

```java
public String ask(String message) {
    return chatClient.prompt()
            .system("Spring 입문자를 가르치는 튜터입니다. 쉬운 한국어로 세 문장 이내로 설명하세요.")
            .user(message)
            .call()
            .content();
}
```

기존 서버를 Ctrl+C로 종료하고 같은 터미널에서 다시 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:bootRun
```

다른 터미널에서 같은 질문을 보냅니다.

```bash
curl -i http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw 'DI가 뭐야?'
```

추가 전후 응답을 비교하세요. 길이·단어 난이도·예시가 달라지는지 관찰합니다.
답변이 세 문장을 넘으면 “코드 오류인가, 모델이 지시를 따르지 않은 것인가?”를 나눠 생각합니다.
최초 프롬프트에 system 한 줄을 추가했으므로 이는 앞서 버전과 정확히 같은 입력은 아닙니다.
교재의 문장 수 지시는 실험용이며 자동 검증이나 보장을 제공하지 않습니다.

실험 후 system 줄을 제거해 원본으로 되돌리세요.
제공된 계약 테스트는 원본의 user 메시지 하나를 검증하므로, 추가한 상태에서는 메시지 개수 검증이 실패하는 것이 자연스럽습니다.
되돌린 후 확인합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test
```

## 6. 대화 기억 실험

원본 서버에서 다음 두 요청을 차례로 보냅니다.

```bash
curl -sS http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '제 이름은 민수입니다.'

curl -sS http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '제 이름이 무엇인가요?'
```

이 코드는 두 번째 호출에 첫 번째 대화를 포함하지 않습니다.
같은 서버·같은 ChatClient를 사용해도 자동으로 대화가 이어지지 않습니다.
모델이 우연히 이름을 맞히는지는 기억 여부의 확실한 증거가 아닙니다.
제공된 `doesNotSendPreviousRequestToLocalMockModelAutomatically` 테스트는 두 번째 외부 요청에 현재 질문만 들어갔는지 확인합니다.
메모리 수업에서는 대화 ID와 이전 메시지를 연결해 이 차이를 해결합니다.

## 7. 프롬프트로 할 일과 코드로 할 일

| 요구 | 우선 담당 | 이유 |
|---|---|---|
| 쉬운 말로 설명 | 프롬프트 | 표현 방식에 대한 요청 |
| 공백 입력 거부 | 코드 | 외부 호출 전에 확실히 검사 가능 |
| 배송비 계산 | 코드·데이터 | 정책과 정확한 연산 필요 |
| 필요한 조회 도구 선택 | 이후 Agent에서 모델 | 상황에 따른 선택이 필요한 경우 |
| 허용되지 않은 도구 실행 방지 | 코드 | 실행 권한은 애플리케이션 책임 |

“검색하고 배송비를 확인하라”는 프롬프트를 써도, 현재 코드에는 검색 도구가 없으므로 실제 검색이 되지 않습니다.
도구를 제공한 후에도 그 지시만으로 실행 순서가 보장되지는 않습니다.
순서를 반드시 보장하려면 Workflow 코드가 해당 순서를 실행해야 합니다.
이 차이는 Tool Calling을 배운 뒤 같은 도구를 사용한 두 구현으로 다시 확인합니다.

## 확인 질문

1. `ChatClient.Builder`를 생성자로 받는 이유는 무엇인가요?
2. user 메시지와 system 메시지는 각각 누구의 어떤 내용을 담나요?
3. system 메시지에 “가격을 틀리지 마라”라고 쓰면 가격 검증 코드가 필요 없을까요?
4. 같은 ChatClient 객체를 재사용하면 이전 질문도 기억하나요?

<details>
<summary>해설과 완료 기준</summary>

자동 설정이 준비한 Builder로 모델 연결을 활용한 ChatClient를 만들기 위해서입니다.
user는 사용자의 요청, system은 애플리케이션의 역할·응답 원칙 등을 담습니다.
지시는 정확성 보장이 아니므로 실제 데이터와 검증 코드가 필요합니다.
ChatClient 재사용만으로 대화 기록이 저장·전달되지는 않습니다.
코드 네 줄을 설명하고 메시지 추가 실험과 기억 실험의 의미를 설명하면 이번 묶음을 마친 것입니다.
</details>

보충 읽기: [Chat Client API](https://docs.spring.io/spring-ai/reference/api/chatclient.html)의 프롬프트 구성과 Builder 부분.

[이전: 02](02-first-call.md) · [다음: 04. 구조화 응답](04-structured-output.md) · [학습 지도](../../README.md)
