# 13. 작은 쇼핑 상담 Agent — 배운 개념 연결하기


먼저 04~12를 학습하고, 각 단원에서 배운 기능을 하나의 상담 흐름으로 연결합니다.
목표는 **상품 추천·정책 검색·후속 질문을 연결하면서 모델 판단과 Java의 책임을 설명하는 것**입니다.
명령은 모두 SpringShoppingAI 저장소 루트에서 실행합니다.
이 단원의 상품 도구는 애플리케이션 내부에서 실행합니다. 원격 도구를 사용하는 세 방식은 [11장 MCP](11-mcp-workflow-agent.md)에서 독립적으로 실행합니다.


## 작은 수업으로 나누어 읽기

처음에는 아래 순서로 하나씩 학습합니다. 전체 코드를 한 번에 이해하거나 보조 코드를 직접 작성할 필요는 없습니다.

| 순서 | 소수업 | 완료 기준 |
|---|---|---|
| 1 | [통합하기 전에 책임 지도 그리기](13-shopping-agent/01-map.md) | 상품 추천·정책 안내·후속 질문을 함께 처리하도록 모델의 판단, 도구 실행, 결과 검증, 대화 저장의 책임과 처리 흐름을 설계합니다. |
| 2 | [의도 추출과 허용 도구 선택](13-shopping-agent/02-intent.md) | 바깥 순서는 Java, 안쪽 도구 선택은 모델이라는 구조를 설명합니다. |
| 3 | [조회한 사실로 최종 결과 확인](13-shopping-agent/03-verify.md) | 상품 총액과 출처 ID를 실제 도구 기록으로 검증합니다. |
| 4 | [대화 저장과 통합 시나리오 관찰](13-shopping-agent/04-conversation.md) | 정상 결과·결과 없음·실패에서 저장 여부를 비교합니다. |

각 소수업은 이 단원의 실습 모듈을 함께 사용합니다. 표의 순서대로 개념과 실행 코드를 연결하세요.
[공통 학습 방법](../learning-guide.md) · [전체 교육 계획](../curriculum.md)

## 실행 코드와 테스트

[labs/13-shopping-agent의 실행 안내](../../labs/13-shopping-agent/README.md)를 확인합니다.
이 단원의 기본 구현은 다음 명령으로 테스트·빌드·실행합니다. 명령은 저장소 루트 기준입니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-shopping-agent:test --tests com.example.ailearning.l13shopping.ShoppingAgentLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:13-shopping-agent:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:13-shopping-agent:bootRun
```
