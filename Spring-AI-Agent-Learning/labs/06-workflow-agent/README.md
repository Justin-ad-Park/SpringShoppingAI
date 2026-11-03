# 실습 06: Workflow / Agent / Hybrid

[학습 문서](../../docs/lessons/06-workflow-agent.md)를 먼저 읽으세요.
05의 상품 데이터와 업무 코드를 이 모듈에 복사했으며 다른 실습을 import하지 않습니다.
읽는 순서: Product/Offer/ProductCatalog 복습 → GiftCriteria → ComparisonService의 extract/workflow/agent/hybrid → TracedShoppingTools → 필요 시 support.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:06-workflow-agent:test
./gradlew :Spring-AI-Agent-Learning:labs:06-workflow-agent:bootRun
```

포트 8096. POST `/api/compare/workflow`, `/api/compare/agent`, `/api/compare/hybrid`는 text/plain 질문을 받습니다.
응답의 status와 steps, clientCalls/modelCalls/toolCalls를 비교하세요.
bootRun은 루트 `.env`의 OPENAI_API_KEY와 OPENAI_MODEL을 읽습니다. 테스트에는 실제 키가 필요 없습니다.
SDK 호출 타임아웃과 전체 요청 대기 상한은 교재에 설명되어 있습니다. 실제 모델에 따라 TIMEOUT이 날 수 있으며 성공처럼 취급하지 않습니다.
