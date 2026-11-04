# 11. 평가와 운영 기초: 좋아졌는지 어떻게 확인할까?

선수 지식: 10의 RAG, HTTP 오류와 Java 예외.
**답변이 좋은가를 평가하는 일**과 **호출이 어떻게 실행됐는지 관찰하는 일**을 나눕니다.
표의 수업을 순서대로 학습하며, 서로 다른 기능을 한 Service에 더하지 않습니다.

| 순서 | 소수업 | 실행 모듈 | 완료 기준 |
|---|---|---|---|
| 1 | [고정 사례로 기준 세우기](11-evaluation/01-cases.md) | `11-evaluation` | 고정 질문·기대 결과와 실제 답변 비교 |
| 2 | [Evaluator로 모델 응답 평가](11-evaluation/02-model-judge.md) | `11-model-evaluation` | 규칙 검사와 평가 모델의 판정 구분 |
| 3 | [같은 응답에서 사용량 읽기](11-evaluation/03-usage.md) | `11-evaluation` | 응답에서 입력·출력 토큰 사용량 읽기 |
| 4 | [Actuator로 기본 메트릭 조회](11-evaluation/04-actuator.md) | `11-observability` | 호출 횟수·시간·토큰 지표 관찰 |
| 5 | [실패·지연·재시도 관찰](11-evaluation/05-failures.md) | `11-evaluation` | 잘못된 답변과 요청 실패 구분 |

실행 명령과 실제 모델/로컬 테스트 구분은 각 수업과 모듈 README에 있습니다.

[공통 학습 방법](../learning-guide.md) · [전체 교육 계획](../curriculum.md)

## 실행 코드와 테스트

[labs/11-evaluation의 실행 안내](../../labs/11-evaluation/README.md)를 확인합니다.
이 단원의 기본 구현은 다음 명령으로 테스트·빌드·실행합니다. 명령은 저장소 루트 기준입니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:11-evaluation:test --tests com.example.ailearning.l11evaluation.EvaluationContractTest
./gradlew :Spring-AI-Agent-Learning:labs:11-evaluation:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:11-evaluation:bootRun
```

표준 API 추가 수업은 표에 적힌 별도 모듈의 README 명령을 사용합니다.
