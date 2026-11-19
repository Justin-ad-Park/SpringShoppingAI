# 13-8. 상담 전체의 모델 요청 수와 대기 시간 제한하기

**목표:** 기본 상담 흐름에 여러 모델 요청을 합친 횟수 제한, 전체 대기 제한, 실행 기록을 추가합니다. 13-1~4 이후의 선택 실습입니다.

기본 앱과 독립된 [13-08-limited-agent](../../../labs/13-08-limited-agent/README.md)를 실행합니다. 이 모듈은 기본 상담의 스냅샷을 별도 패키지로 보관하므로 기본 코드가 확장에 의존하지 않습니다. 의도·도구·검증·메모리 동작은 같고 실행 제한과 trace가 추가됩니다.

## 실행 상한과 기록

기본 상담을 익힌 뒤 별도 모듈에서 실행 제한을 추가합니다. RunState·실행기·지연 응답 검사는 이 확장에만 있습니다.

| 기본 설정 | 값 | 의미 |
|---|---:|---|
| `learning.limits.max-model-calls` | 8 | 한 쇼핑 에이전트 요청 안의 모델 호출 상한 |
| `learning.limits.timeout-ms` | 20000 | 모델·도구 흐름을 포함한 전체 요청의 최대 실행 시간 |
| `spring.ai.openai.timeout` | 5s | OpenAI API 요청 한 번의 시간 제한 |
| `spring.ai.tools.limits.max-total-tool-calls` | 6 | 한 도구 호출 루프에서 모든 도구를 합친 실행 상한 |
| `spring.ai.openai.chat.max-retries` | 0 | Chat API 자동 재시도 없음 |

모델 API 타임아웃은 Spring AI 표준 설정으로 제한합니다. 전체 에이전트 요청은 의도 추출·도구 선택·도구 실행을 여러 번 거치므로 별도 실행기가 `learning.limits.timeout-ms`를 적용합니다. 두 시간 제한은 범위가 다릅니다.
이 Workflow에는 도구를 사용하는 단계가 하나이므로 도구 횟수 상한은 Spring AI의 전체 도구 제한으로 적용합니다. `RunState`는 실제 실행된 도구 수를 응답 trace에 기록하지만 같은 제한을 다시 검사하지 않습니다. 도구 호출 단계가 여러 개로 늘어나 한 HTTP 요청 전체에 걸쳐 누적 상한을 적용해야 한다면 애플리케이션 카운터가 추가로 필요합니다. 설정 방법은 [05장 도구 호출 제한](../05-tool-calling/05-tool-call-limits.md)을 참고하세요.

HTTP 요청의 대기열·전송 시간을 포함한 전체 SLA를 보장하는 설정은 아닙니다.
작업 취소를 요청해도 외부 제공 업체가 이미 수행한 요청과 비용까지 취소되지는 않을 수 있습니다.
`trace.clientCreations`는 이 상담에서 생성한 `ChatClient` 인스턴스 수입니다. `trace.modelRequestAttempts`는 `ChatModel.call()` 시작 횟수이며 제공 업체 SDK 내부의 HTTP 재시도는 포함하지 않습니다. `trace.toolExecutions`는 Spring AI 상한을 통과해 실제 도구 코드가 시작된 횟수입니다.
임베딩 요청은 모델 요청 시도 수에 포함하지 않습니다. 기록은 도구 요청과 반환값이며 모델의 내부 사고가 아닙니다.


## 어떤 코드가 추가될까?

| 코드 | 추가 책임 |
|---|---|
| [LimitedAgentRequestExecutor](../../../labs/13-08-limited-agent/src/main/java/com/example/ailearning/l13limited/support/LimitedAgentRequestExecutor.java) | 별도 작업으로 실행하고 전체 대기 시간 초과 시 취소 요청 |
| [RunState](../../../labs/13-08-limited-agent/src/main/java/com/example/ailearning/l13limited/support/RunState.java) | 여러 단계의 모델 요청 시도 수·마감 확인과 실행 내역 보관 |
| [CounselWorkflow](../../../labs/13-08-limited-agent/src/main/java/com/example/ailearning/l13limited/workflow/CounselWorkflow.java) | 호출 계측을 위한 ChatModel 포장과 도구 상한 응답 확인 |
| [CounselService](../../../labs/13-08-limited-agent/src/main/java/com/example/ailearning/l13limited/CounselService.java) | 실행 실패·시간 초과이면 새 대화를 저장하지 않음 |

Spring AI의 API 요청 타임아웃과 도구 호출 상한을 재구현한 것이 아닙니다. 사용자 요청 전체를 제한하는 자체 코드와 제공되는 요청별 설정을 구분합니다.

## 테스트와 실행

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-08-limited-agent:test --tests '*LocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:13-08-limited-agent:bootRun
```

[LimitedShoppingAgentLocalModelTest](../../../labs/13-08-limited-agent/src/test/java/com/example/ailearning/l13limited/LimitedShoppingAgentLocalModelTest.java)는 기존 상담과 함께 모델 상한·도구 상한·시간 초과 뒤 늦은 답변 미저장을 검증합니다. 외부 모델은 호출하지 않습니다. `bootRun`과 [Live](../../../labs/13-08-limited-agent/src/test/java/com/example/ailearning/l13limited/LimitedShoppingAgentOpenAiLiveTest.java)는 루트 `.env`를 사용하며 실제 채팅·임베딩 API를 호출합니다. 포트는 8118, 경로는 `/api/counsel/demo`입니다. 종료는 Ctrl+C입니다.

## 확인 질문

모델 API 요청 한 번의 타임아웃만으로 상담 전체의 시간이 제한되나요?

<details><summary>해설</summary>
아닙니다. 상담은 의도 추출·여러 도구·후속 모델 요청을 거칩니다. 전체 대기는 실행기가 별도로 제한하며, 취소 요청으로 이미 발생한 외부 비용까지 취소되는 것은 아닙니다. 실패한 결과는 기본 대화 메모리에 추가하지 않습니다.
</details>

[기본 상담으로 돌아가기](04-conversation.md) · [13장 목차](../13-shopping-agent.md)
