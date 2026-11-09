# 12-3. 같은 응답에서 사용량 읽기

**실행 코드·설정·테스트:** [labs/12-00-evaluation](../../../labs/12-00-evaluation/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 모델 응답에서 입력·출력 토큰 사용량을 읽는 방법을 이해합니다.

**코드 읽기 안내:** 입력·출력 토큰은 제공 업체가 응답한 metadata입니다. 값이 없을 때 임의로 0으로 만드는 것과 구분합니다.

## 한 번 호출해서 객체와 사용량을 함께 받기

10의 `.entity(DraftAnswer.class)`를 `.responseEntity(DraftAnswer.class)`로 바꿉니다.
이 Spring AI 타입은 변환한 객체와 원래 ChatResponse를 함께 담습니다. Spring MVC의 HTTP ResponseEntity와는 다른 타입입니다.

```java
var response = client
        .prompt()
        .system("제공한 정책 근거 안에서만 답하세요.")
        .user(questionAndContext)
        .call()
        .responseEntity(DraftAnswer.class);
DraftAnswer draft = response.entity();
var usage = response
        .response()
        .getMetadata()
        .getUsage();
```

위 코드는 새 API를 설명하기 위한 부분 예제입니다. 질문과 근거를 조립하는 실제 실행 코드는 [RagService.answerInternal()](../../../labs/12-00-evaluation/src/main/java/com/example/ailearning/l12evaluation/RagService.java)에 있습니다. 실제 소스는 `questionAndContext` 변수 대신 문자열을 직접 조립합니다.
토큰을 읽으려고 모델을 두 번 호출하지 않습니다.

### 사용량을 응답 필드로 옮기기

같은 메서드에서 사용량을 아래처럼 옮깁니다. 실제 코드의 `Stats`에는 `Integer input, output` 필드가 있고 초기값은 null입니다.

```java
if (usage != null && usage.getTotalTokens() > 0) {
    stats.input = usage.getPromptTokens();
    stats.output = usage.getCompletionTokens();
}
```

이 예제는 보고된 전체 토큰이 양수일 때만 값을 채웁니다. usage가 없거나 응답 변환 전에 실패하면 null을 유지하며, JSON 설정에 따라 필드가 생략될 수 있습니다.
null은 사용량을 확인하지 못했다는 뜻이며 0회 사용으로 바꾸지 않습니다. 토큰 필드는 채팅 응답에서 보고된 값만 담습니다.

### 검색과 생성을 합친 시간은 별도로 측정하기

다음은 같은 클래스의 시간 측정 위치를 보여주는 발췌입니다. 성공 결과를 만드는 경로만 표시했고 예외 처리는 [12-5](05-failures.md)에서 읽습니다.

```java
long started = System.nanoTime();
var stats = new Stats();
Reply reply = answerInternal(question, stats); // 질문 임베딩·검색, 필요하면 채팅 생성
return new Run(reply, metrics(started, stats, null));
```

`metrics()`는 `(System.nanoTime() - started) / 1_000_000`을 `elapsedMs`로 반환합니다.
따라서 `elapsedMs`는 검색과 생성 전체 시간이고, 입력·출력 토큰은 그중 채팅 응답의 사용량입니다.
실제 응답은 [12-5 실행 명령](05-failures.md#실행)의 `/api/evaluation/ask`를 호출해 `metrics` 필드에서 확인합니다.


## 확인 질문

사용량을 읽는 코드와 경과 시간을 계산하는 코드가 각각 어떤 범위를 측정하는지 설명해 보세요.

<details>
<summary>해설</summary>

`response.getMetadata().getUsage()`는 해당 채팅 응답이 보고한 입력·출력 토큰을 읽습니다. `System.nanoTime()`으로 계산하는 `elapsedMs`는 질문 임베딩·검색과 필요한 채팅 생성을 포함한 처리 시간을 측정합니다. 채팅 토큰 수에 임베딩 사용량이 합산되는 것은 아닙니다.

</details>

[이전](02-model-judge.md) · [다음](04-actuator.md) · [단원 목차](../12-evaluation.md) · [학습 방법](../../learning-guide.md)
