# 11. MCP 도구를 사용하는 Workflow·Agent·하이브리드

목표는 **외부 서버의 도구를 연결하고, 같은 도구를 사용할 때 Java와 모델의 판단 범위가 어떻게 달라지는지 설명하고 실행하는 것**입니다.
선수 지식은 04 구조화 응답, 05 도구 호출, 06 세 실행 방식입니다. RAG는 MCP의 필수 조건이 아닙니다.

| 순서 | 소수업 | 완료 기준 |
|---|---|---|
| 1 | [MCP는 무엇을 연결할까?](11-mcp-workflow-agent/01-connection.md) | 모델·클라이언트·서버의 역할과 실행 위치 구분 |
| 2 | [모델 없이 MCP 도구 호출](11-mcp-workflow-agent/02-direct-call.md) | MCP 도구 목록을 조회하고 지정한 도구로 배송 정책 받기 |
| 3 | [Workflow: Java가 순서를 정하기](11-mcp-workflow-agent/03-workflow.md) | 검색 결과 전체의 ID를 계산 도구에 전달 |
| 4 | [Agent: 모델이 도구 선택하기](11-mcp-workflow-agent/04-agent.md) | 도구 설명·입력 스키마와 실제 호출 기록 연결 |
| 5 | [하이브리드: Java의 검증 결합](11-mcp-workflow-agent/05-hybrid.md) | 검색 조건·전체 상품 계산·총예산 검증 |
| 6 | [같은 요청으로 비교하고 테스트하기](11-mcp-workflow-agent/06-compare.md) | 실행 기록으로 세 방식의 보장 범위 설명 |

기본 실습은 같은 요청을 비교합니다.

> 음료 중 상품 가격 15,000원 이하인 MALL 상품을 모두 한 개씩 구매할 때 배송비 포함 총액을 알려줘. 총예산은 30,000원이야.

현재 서버의 상품은 허브티 P004(9,900원)와 드립 커피 P005(12,500원)입니다.
빈 데모 장바구니에서 계산하면 상품 합계 22,400원 + 배송비 3,000원 = **25,400원**입니다.
`quoteCart`는 이 계산만 수행하고 실제 장바구니를 변경하지 않습니다. 06장의 상품별 배송비와 달리 이 장에서는 장바구니 전체의 배송비를 계산합니다.

## 실행 코드와 테스트

실행 모듈은 [labs/11-mcp-workflow-agent](../../labs/11-mcp-workflow-agent/README.md)입니다.
로컬 테스트는 MCP 서버를 자동 실행하고 OpenAI 응답만 고정합니다. 수동 실행은 MCP 서버와 학습 애플리케이션을 각각 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:11-mcp-workflow-agent:test --tests '*LocalModelTest' --tests '*UnitTest'
./gradlew :Spring-AI-Agent-Learning:labs:11-mcp-workflow-agent:bootJar
```

[이전: 10장 RAG](10-rag.md) · [다음: 12장 평가와 운영](12-evaluation.md) · [전체 교육 계획](../curriculum.md)
