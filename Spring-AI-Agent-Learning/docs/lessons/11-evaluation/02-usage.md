# 11-2. 같은 응답에서 사용량 읽기

30~45분을 기준으로 한 번에 한 가지 흐름만 읽습니다. 단원 전체 시간은 목차의 기존 예상 시간에 포함됩니다.

**이번 목표:** 답변과 usage를 받기 위해 두 번 호출할 필요가 없음을 확인합니다.

**코드 읽기 안내:** 입력·출력 토큰은 제공 업체가 응답한 metadata입니다. 값이 없을 때 임의로 0으로 만드는 것과 구분합니다.

## 한 번 호출해서 객체와 사용량을 함께 받기

10의 `.entity(DraftAnswer.class)`를 `.responseEntity(DraftAnswer.class)`로 바꿉니다.
이 Spring AI 타입은 변환한 객체와 원래 ChatResponse를 함께 담습니다. Spring MVC의 HTTP ResponseEntity와는 다른 타입입니다.

```java
var response = client.prompt()
        .system("제공한 정책 근거 안에서만 답하세요.")
        .user(questionAndContext)
        .call().responseEntity(DraftAnswer.class);
DraftAnswer draft = response.entity();
var usage = response.response().getMetadata().getUsage();
```

위 코드는 새 API를 설명하기 위한 부분 예제입니다. `questionAndContext`를 만드는 실제 실행 코드는 RagService에 있습니다.
토큰을 읽으려고 모델을 두 번 호출하지 않습니다.
usage가 없거나 파싱 전에 실패하면 토큰은 null이며, JSON 설정에 따라 필드가 생략될 수 있습니다. 0으로 채워 실제 사용량이 없었다고 표시하지 않습니다.
`elapsedMs`에는 검색과 생성 전체 시간이 포함되지만 토큰 필드는 **채팅 응답에서 보고된 값만** 포함됩니다.


## 여기서 멈춰 확인하기

코드를 닫고 “이번 입력 → 새로 하는 처리 → 확인할 결과”를 한 문장으로 적어 보세요.
설명하기 어렵다면 위 핵심 메서드만 다시 읽습니다. 테스트용 HTTP 서버 구현을 이해해야 다음으로 갈 수 있는 것은 아닙니다.

[이전](01-cases.md) · [다음 소수업](03-failures.md) · [학습 방법](../../learning-guide.md)

[다음: Actuator 메트릭 실행 실습](05-actuator.md)
