# 12. 평가와 운영 기초: 좋아졌는지 어떻게 확인할까?

## 이번 수업의 목표

선수 지식은 10장의 RAG, HTTP 오류와 Java 예외입니다.

- 고정 사례·기대 결과를 정하고 같은 기준으로 변경 전후를 비교합니다.
- 코드 검사와 평가 모델의 판정이 각각 무엇을 확인하는지 설명합니다.
- 응답의 토큰 사용량과 호출 메트릭을 읽고 품질·실패·지연을 구분합니다.
- 로컬 테스트 통과와 실제 모델의 품질 평가를 구분합니다.

## 개념 먼저 이해하기

AI 기능은 연결이 성공했다는 사실만으로 품질을 판단하기 어렵습니다. **평가(Evaluation)**는 답변이 요구사항에 맞는지 확인하는 일이고, **관측(Observability)**은 서비스가 어떤 상태로 동작했는지 확인할 수 있도록 실행 정보를 모으고 살펴보는 일입니다.

“반품 기간은 7일”이라는 근거를 줬는데 모델이 30일이라고 답하면 품질 문제입니다. 답변 내용은 맞지만 지나치게 늦거나 요청이 실패한다면 운영 문제입니다. 두 관점을 함께 확인해야 서비스를 개선할 수 있습니다.

| 용어 | 쉬운 설명 | 확인할 질문 |
|---|---|---|
| 평가 사례 | 입력과 기대하는 결과를 함께 정한 예 | 근거가 없을 때 답변을 보류하는가? |
| Evaluator(평가 도구) | 정한 기준에 따라 답변을 검사하는 기능 | 질문과 관련 있고 근거에 맞는가? |
| 관측 | 실행 중 생긴 정보를 모아 동작을 파악하는 일 | 어디서 실패했고 얼마나 걸렸는가? |
| 메트릭(metric) | 횟수·시간·사용량 등을 수치로 측정한 값 | 실패율이나 응답 시간이 늘고 있는가? |
| 토큰 사용량(usage) | 모델이 처리한 입력·출력의 토큰 수 | 같은 기능에서 처리할 정보량이 얼마나 늘었는가? |

Spring AI는 평가 도구와 모델 호출 관련 측정 기능을 제공합니다. **Actuator(액추에이터)**는 애플리케이션의 상태와 측정값을 조회하도록 돕는 Spring Boot 기능입니다. 토큰 사용량은 비용을 파악할 때 참고할 정보이며, 실제 요금은 모델과 제공 업체의 과금 조건에 따라 달라집니다.

평가 모델을 사용한 판정도 다른 모델의 판단이므로, 점수 하나가 정확성을 보증하지는 않습니다. 중요한 질문, 답변해도 되는 조건, 근거가 없을 때의 기대 행동을 먼저 정하고 사람이 확인할 기준과 연결해야 합니다.

02장 보충의 타임아웃·재시도와 05장 보충의 도구 호출 제한 실습도 여기서 다시 활용합니다. 서비스가 질문을 한 번 처리하는 동안 재시도로 실제 모델 서버에는 여러 번 요청할 수 있으므로, 어떤 단계의 횟수와 시간을 측정하는지 구분합니다.

개념 참고: [Spring AI의 평가](https://docs.spring.io/spring-ai/reference/api/testing.html), [관측 기능](https://docs.spring.io/spring-ai/reference/observability/index.html).

## 실습 안내

### 평가·관측 API를 코드와 연결하기

| API | 역할 | 이 수업에서의 위치 |
|---|---|---|
| `Evaluator` | 평가 요청을 받고 평가 결과를 반환하는 인터페이스 | 12-2의 평가 기능 계약 |
| `EvaluationRequest` | 질문·근거·후보 답변을 담는 입력 | 평가할 자료 구성 |
| `RelevancyEvaluator` | 질문·근거와 답변의 관련성 평가 | 후보 답변을 모델로 판정 |
| `FactCheckingEvaluator` | 제공된 근거와 답변 주장의 일치 평가 | 외부 사실 전체를 검증하는 검색기가 아님 |
| `EvaluationResponse` | 통과 여부 등 평가 결과 | `isPass()`로 판정 읽기 |
| 응답 metadata의 `Usage` | 모델이 보고한 토큰 사용량 | 12-3에서 읽기 |
| Micrometer / Actuator | 호출 측정과 메트릭 조회 | 12-4의 별도 관측 실습 |

### 한 답변을 두 관점에서 보기

정책은 “7일”인데 답변이 “30일”이면 내용 검사를 해야 합니다. 반면 답변이 맞아도 20초가 걸리거나 호출이 실패하면 지연·실패를 측정해야 합니다. 평가 모델 호출 역시 시간과 비용이 드는 별도 호출입니다. 답변 생성 사용량만 보고 평가까지 포함한 전체 비용이라고 해석하지 않습니다.

실무에서는 변경 전후를 같은 질문·근거로 비교하고 모델·프롬프트·문서 버전을 기록합니다. 평균 점수 외에 실패 사례와 중요한 업무별 결과도 확인합니다. 반복 평가와 운영 측정의 설계는 [12-6 선택 보충](12-evaluation/06-production-evaluation.md)에 있습니다.

선수 지식: 10의 RAG, HTTP 오류와 Java 예외.
**답변이 좋은가를 평가하는 일**과 **호출이 어떻게 실행됐는지 관찰하는 일**을 나눕니다.
표의 수업을 순서대로 학습하며, 서로 다른 기능을 한 Service에 더하지 않습니다.

| 순서 | 소수업 | 실행 모듈 | 완료 기준 |
|---|---|---|---|
| 1 | [고정 사례로 기준 세우기](12-evaluation/01-cases.md) | `12-00-evaluation` | 고정 질문·기대 결과와 실제 답변 비교 |
| 2 | [Evaluator로 모델 응답 평가](12-evaluation/02-model-judge.md) | `12-02-model-evaluation` | 규칙 검사와 평가 모델의 판정 구분 |
| 3 | [같은 응답에서 사용량 읽기](12-evaluation/03-usage.md) | `12-00-evaluation` | 응답에서 입력·출력 토큰 사용량 읽기 |
| 4 | [Actuator로 기본 메트릭 조회](12-evaluation/04-actuator.md) | `12-04-observability` | 호출 횟수·시간·토큰 지표 관찰 |
| 5 | [실패·지연·재시도 관찰](12-evaluation/05-failures.md) | `12-00-evaluation` | 잘못된 답변과 요청 실패 구분, 02·05 보충 테스트로 제약·재시도 결과 비교 |
| 선택 6 | [평가 결과로 변경을 판단하고 운영에서 관찰하기](12-evaluation/06-production-evaluation.md) | 설계 보충 | 고정 사례·반복 평가·운영 지표를 함께 해석 |

실행 명령과 실제 모델/로컬 테스트 구분은 각 수업과 모듈 README에 있습니다.

[공통 학습 방법](../learning-guide.md) · [전체 교육 계획](../curriculum.md)

## 실행 코드와 테스트

[labs/12-00-evaluation의 실행 안내](../../labs/12-00-evaluation/README.md)를 확인합니다.
이 단원의 기본 구현은 다음 명령으로 테스트·빌드·실행합니다. 명령은 저장소 루트 기준입니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:12-00-evaluation:test --tests com.example.ailearning.l12evaluation.EvaluationLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:12-00-evaluation:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:12-00-evaluation:bootRun
```

표준 API 추가 수업은 표에 적힌 별도 모듈의 README 명령을 사용합니다.

## 확인 질문

1. `EvaluationResponse.isPass()`가 true이면 근거 문서 자체도 사실이라고 보장되나요?
2. API 요청 성공률이 높으면 답변 품질도 높다고 볼 수 있나요?
3. 생성 모델과 평가 모델을 모두 호출했다면 비용·지연을 어떻게 구분해서 봐야 하나요?

<details>
<summary>해설</summary>

1. 아닙니다. 평가 기준에 따른 모델의 판정입니다. 잘못된 원문에 맞는 답변도 통과할 수 있습니다.
2. 아닙니다. 연결과 실행 성공은 답변 내용의 정확성과 다릅니다. 품질 평가와 운영 지표를 함께 확인해야 합니다.
3. 각 호출의 사용량과 시간을 구분하고 서비스 전체에서 합산할 범위를 명시합니다. 임베딩이나 재시도까지 있다면 빠진 호출이 없는지도 확인합니다.

</details>

## 참고 자료

- [Spring AI Evaluation](https://docs.spring.io/spring-ai/reference/api/testing.html) · [Observability](https://docs.spring.io/spring-ai/reference/observability/index.html): 평가 API와 측정 범위의 기준입니다.
- [Canonical / Ubuntu — Spring AI and Observability](https://ubuntu.com/developers/docs/tutorials/java-advanced/spring-ai/springai-observability/): RAG 실행과 메트릭 조회를 연결하는 실습 구성을 참고했습니다. 해당 자료의 Prometheus 구성을 현재 실습에 추가한 것은 아닙니다.
- [Anthropic — Demystifying evals for AI agents](https://www.anthropic.com/research/demystifying-evals-for-ai-agents): 여러 평가 기준·반복 실행·실제 결과를 분리하는 방법을 참고했습니다.
- [07~13장 참고 자료 안내](../references.md#0713장-보강에-사용한-교육-자료)

[이전: 11장 MCP](11-mcp-workflow-agent.md) · [다음: 13장 통합](13-shopping-agent.md)
