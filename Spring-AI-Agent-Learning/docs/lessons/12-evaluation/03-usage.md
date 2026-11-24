# 12-3. 응답 한 번에서 답변과 토큰 사용량 읽기

**이번 수업의 목표:** 입력·출력 토큰의 의미를 이해하고, Spring AI의 모델 응답 한 번에서 답변과 사용량을 함께 읽습니다.<br>
출력값을 해석하고 질문·답변 길이에 따른 사용량을 비교합니다.

**실행 코드·설정·테스트:** [12-00-evaluation](../../../labs/12-00-evaluation/README.md)의 `usage-only` 실습을 사용합니다.<br>
02단원의 ChatClient 호출에서 시작하며 RAG 없이 실행할 수 있습니다.

## 답변 외에 사용량도 읽는 이유

같은 질문에도 한 문장으로 답할 때와 길게 설명할 때 사용량이 달라집니다.<br>
프롬프트를 바꾼 뒤 사용량을 기록하면 더 많은 문맥이나 긴 답변이 자원 사용에 어떤 영향을 주는지 비교할 수 있습니다.<br>
답변이 맞는지 평가하는 것과 사용량을 측정하는 것은 별개입니다.

모델 API는 보통 토큰 사용량 등을 기준으로 비용을 계산합니다.<br>
이 수업에서는 토큰 수를 읽으며 금액으로 환산하지 않습니다.<br>
실제 비용 계산에는 선택한 모델의 과금 기준을 별도로 적용해야 합니다.

## 먼저 알아둘 용어

| 용어 | 뜻 | 이 실습에서 읽는 값 |
|---|---|---|
| 토큰(token) | 모델이 텍스트를 처리하는 단위. 한 글자나 한 단어와 정확히 일치하지 않음 | 사용량의 개수를 세는 단위 |
| 입력 토큰 | 모델에 전달한 입력을 처리한 토큰 수 | `getPromptTokens()` |
| 출력 토큰 | 모델이 생성한 응답의 토큰 수 | `getCompletionTokens()` |
| 사용량(Usage) | 제공 업체가 응답에 보고한 사용량 정보 | `getMetadata().getUsage()` |
| 응답 메타데이터 | 답변 본문 외에 모델 응답에 딸린 부가 정보 | 이 중 사용량을 읽음 |

입력에는 사용자 질문뿐 아니라 시스템 지시나 함께 보낸 이전 대화·참고 자료도 들어갈 수 있습니다.<br>
이 기본 예제는 `.user(question)`으로 질문을 전달합니다.<br>
글자 수로 토큰 수를 정확히 계산할 수는 없으며 모델·토큰화 방식과 실제 요청 구성에 따라 달라집니다.

## 답변 문자열 대신 ChatResponse 받기

02단원의 `.call().content()`는 답변 문자열을 바로 읽는 방법입니다.<br>
이번에는 `.call().chatResponse()`로 답변과 메타데이터를 담은 `ChatResponse`를 받습니다.<br>
답변과 사용량을 읽으려고 모델을 각각 호출할 필요가 없습니다.


| API | 읽는 값 |
|---|---|
| `call().chatResponse()` | 모델 응답 한 번 |
| `getResult().getOutput().getText()` | 답변 텍스트 |
| `getMetadata().getUsage()` | 제공 업체가 보고한 토큰 사용량 |

## 한 응답에서 답변과 사용량을 꺼내기

[Lesson03TokenUsageService](../../../labs/12-00-evaluation/src/main/java/com/example/ailearning/l12evaluation/lesson03_usage/Lesson03TokenUsageService.java)의 전체 코드입니다.

```java
package com.example.ailearning.l12evaluation.lesson03_usage;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** 12-3: 채팅 응답 한 번에서 답변과 토큰 수를 함께 읽는다. */
@Service
public class Lesson03TokenUsageService {
    public record Reply(String answer, Integer inputTokens, Integer outputTokens) {}
    private final ChatClient chatClient;

    public Lesson03TokenUsageService(ChatClient.Builder builder) {
        chatClient = builder.build();
    }

    public Reply ask(String question) {
        var response = chatClient
                .prompt()
                .user(question)
                .call()
                .chatResponse();
        var usage = response.getMetadata().getUsage();
        boolean reported = usage != null && usage.getTotalTokens() > 0;
        return new Reply(response.getResult().getOutput().getText(),
                reported ? usage.getPromptTokens() : null,
                reported ? usage.getCompletionTokens() : null);
    }
}
```

`ask()`는 다음 순서로 처리합니다.

1. `.user(question)`으로 질문을 구성하고 `.call().chatResponse()`로 응답을 받습니다.
2. `response.getMetadata().getUsage()`로 그 응답의 사용량을 읽습니다.
3. `getResult().getOutput().getText()`로 답변을 읽고, 입력·출력 토큰과 함께 샘플의 `Reply`에 담습니다.

`reported`는 이 샘플에서 사용량을 읽을 수 있다고 판단하는 조건입니다.<br>
usage가 없거나 전체 토큰이 양수로 보고되지 않으면 두 필드를 `null`로 둡니다.<br>
`null`은 확인하지 못했다는 뜻이며 사용량 0과 다릅니다.<br>
이 예제의 사용량에는 다른 요청의 임베딩·평가 호출이 포함되지 않습니다.

## 실행과 확인

명령은 저장소 루트에서 실행합니다.<br>
로컬 테스트는 키 없이 고정 응답을 사용합니다.<br>
실제 실행은 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`을 준비합니다.<br>
설정은 [실행 안내](../../run-guide.md)를 참고하세요.

`usage-only` 프로필은 기존 RAG·정책 검색·통합 평가 Controller를 제외합니다.<br>
`--spring.ai.model.embedding=none`은 이 실습에서 사용하지 않는 임베딩 모델의 자동 구성을 끕니다.<br>
서버 실행 후 `curl`은 다른 터미널에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:12-00-evaluation:test --tests '*Lesson03TokenUsageLocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:12-00-evaluation:bootRun --args='--spring.profiles.active=usage-only --spring.ai.model.embedding=none'
curl http://127.0.0.1:8101/api/evaluation/usage -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '안녕'
```

[Lesson03TokenUsageLocalModelTest](../../../labs/12-00-evaluation/src/test/java/com/example/ailearning/l12evaluation/lesson03_usage/Lesson03TokenUsageLocalModelTest.java)는 고정 답변과 10/5 토큰을 받아 채팅 요청 한 번, 임베딩 요청 없음까지 검증합니다.<br>
외부 모델은 호출하지 않습니다.<br>
`bootRun`은 루트 `.env`의 키·채팅 모델로 실제 API를 호출합니다.<br>
종료는 Ctrl+C입니다.

## 출력값을 어떻게 해석할까?

위 로컬 테스트의 고정 응답 예시는 다음과 같습니다.

```json
{"answer":"반갑습니다.","inputTokens":10,"outputTokens":5}
```

| 값 | 해석 |
|---|---|
| `answer` | 모델 응답에서 읽은 답변 텍스트 |
| `inputTokens: 10` | 제공 업체 역할의 고정 서버가 입력 사용량을 10으로 보고함 |
| `outputTokens: 5` | 같은 서버가 출력 사용량을 5로 보고함 |

10과 5는 “안녕”과 “반갑습니다.”를 실제 토큰화해 계산한 값이 아니라 테스트 서버에 지정한 값입니다.<br>
테스트는 보고된 사용량을 올바르게 읽는지 확인합니다.<br>
실제 모델에서는 같은 숫자를 기대하지 않습니다.<br>
사용량이 작다는 사실만으로 답변 품질이 좋다고 판단하지도 않습니다.

## 실제 모델로 비교해 보기

서버를 실행한 상태에서 같은 주제를 두 가지 길이로 요청합니다.<br>
두 요청 모두 실제 API 사용량이 발생합니다.

```bash
curl http://127.0.0.1:8101/api/evaluation/usage -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '무료배송의 의미를 한 문장으로 설명해줘.'
curl http://127.0.0.1:8101/api/evaluation/usage -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '무료배송의 의미를 예시 세 가지와 함께 자세히 설명해줘.'
```

같은 모델에서 `answer`의 길이와 `inputTokens`, `outputTokens`를 함께 기록합니다.<br>
두 번째 지시는 질문 자체도 달라 입력 토큰도 달라질 수 있습니다.<br>
출력 토큰이 반드시 일정 비율로 증가한다고 가정하지 말고, 실제 답변을 더 길게 생성했는지와 보고된 사용량을 대조하세요.<br>
위 요청은 개념 설명이며 이 서비스가 실제 쇼핑몰의 무료배송 정책을 조회하는 것은 아닙니다.

다음 단계인 [12-5](05-failures.md)에서는 구조화 답변·사용량·전체 검색 및 생성 시간·실패 상태를 결합합니다.<br>
채팅 응답의 토큰 수와 전체 처리 시간은 측정 범위가 다릅니다.

실제 모델 연결은 [Lesson03TokenUsageOpenAiLiveTest](../../../labs/12-00-evaluation/src/test/java/com/example/ailearning/l12evaluation/lesson03_usage/Lesson03TokenUsageOpenAiLiveTest.java)를 별도로 실행해 확인합니다.<br>
루트 `.env`가 필요하며 API 사용량이 발생합니다.

## 확인 질문

1. `content()` 대신 `chatResponse()`를 받으면 이 수업에서 무엇을 함께 읽을 수 있나요?
2. 질문 글자 수와 입력 토큰 수는 항상 같나요? 이전 대화를 함께 전달하면 어떤 입력이 늘어나나요?
3. 사용량 필드가 `null`이면 토큰을 사용하지 않았다는 뜻인가요?

<details>
<summary>해설</summary>

1. 답변 텍스트와 응답 메타데이터의 사용량을 함께 읽습니다.<br>
   같은 ChatResponse를 읽으므로 이를 위해 모델을 두 번 호출하지 않습니다.
2. 같지 않습니다.<br>
   토큰은 글자와 일대일 대응하지 않습니다.<br>
   이전 대화도 모델에 보낸 입력이므로 해당 문맥의 토큰이 입력 사용량에 반영될 수 있습니다.
3. 아닙니다.<br>
   이 샘플이 사용량을 확인하지 못했다는 뜻입니다.<br>
   보고되지 않은 값을 사용량 0으로 해석하면 안 됩니다.

</details>

[이전](02-model-judge.md) · [목차](../12-evaluation.md) · [다음](04-actuator.md)
