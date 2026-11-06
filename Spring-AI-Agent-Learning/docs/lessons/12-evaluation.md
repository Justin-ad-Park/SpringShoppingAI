# 12. 평가와 운영 기초: 좋아졌는지 어떻게 확인할까?

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

06장의 호출 제한·타임아웃·재시도 실습도 여기서 다시 활용합니다. 서비스가 질문을 한 번 처리하는 동안 재시도로 실제 모델 서버에는 여러 번 요청할 수 있으므로, 어떤 단계의 횟수와 시간을 측정하는지 구분합니다.

개념 참고: [Spring AI의 평가](https://docs.spring.io/spring-ai/reference/api/testing.html), [관측 기능](https://docs.spring.io/spring-ai/reference/observability/index.html).

## 실습 안내
선수 지식: 10의 RAG, HTTP 오류와 Java 예외.
**답변이 좋은가를 평가하는 일**과 **호출이 어떻게 실행됐는지 관찰하는 일**을 나눕니다.
표의 수업을 순서대로 학습하며, 서로 다른 기능을 한 Service에 더하지 않습니다.

| 순서 | 소수업 | 실행 모듈 | 완료 기준 |
|---|---|---|---|
| 1 | [고정 사례로 기준 세우기](12-evaluation/01-cases.md) | `12-evaluation` | 고정 질문·기대 결과와 실제 답변 비교 |
| 2 | [Evaluator로 모델 응답 평가](12-evaluation/02-model-judge.md) | `12-model-evaluation` | 규칙 검사와 평가 모델의 판정 구분 |
| 3 | [같은 응답에서 사용량 읽기](12-evaluation/03-usage.md) | `12-evaluation` | 응답에서 입력·출력 토큰 사용량 읽기 |
| 4 | [Actuator로 기본 메트릭 조회](12-evaluation/04-actuator.md) | `12-observability` | 호출 횟수·시간·토큰 지표 관찰 |
| 5 | [실패·지연·재시도 관찰](12-evaluation/05-failures.md) | `12-evaluation` | 잘못된 답변과 요청 실패 구분, 06 확장 테스트로 제약·재시도 결과 비교 |

실행 명령과 실제 모델/로컬 테스트 구분은 각 수업과 모듈 README에 있습니다.

[공통 학습 방법](../learning-guide.md) · [전체 교육 계획](../curriculum.md)

## 실행 코드와 테스트

[labs/12-evaluation의 실행 안내](../../labs/12-evaluation/README.md)를 확인합니다.
이 단원의 기본 구현은 다음 명령으로 테스트·빌드·실행합니다. 명령은 저장소 루트 기준입니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:12-evaluation:test --tests com.example.ailearning.l12evaluation.EvaluationLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:12-evaluation:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:12-evaluation:bootRun
```

표준 API 추가 수업은 표에 적힌 별도 모듈의 README 명령을 사용합니다.

[이전: 11장 MCP](11-mcp-workflow-agent.md) · [다음: 13장 통합](13-shopping-agent.md)
