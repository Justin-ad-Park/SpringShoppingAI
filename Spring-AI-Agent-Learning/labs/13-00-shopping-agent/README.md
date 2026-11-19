# 13 기본 쇼핑 상담

[교재](../../docs/lessons/13-shopping-agent.md)의 13-1~4를 실행합니다. 일반 ChatClient로 의도 추출 → 도구 선택 → 실제 조회 결과 검증 → 대화 저장을 연결합니다.

| 수업 | 코드 | 검증 |
|---|---|---|
| 13-1·2 | [CounselWorkflow](src/main/java/com/example/ailearning/l13shopping/workflow/CounselWorkflow.java) | [ShoppingAgentLocalModelTest](src/test/java/com/example/ailearning/l13shopping/ShoppingAgentLocalModelTest.java): 동기 직접 호출·도구 제공 범위 |
| 13-3 | [CounselResultVerifier](src/main/java/com/example/ailearning/l13shopping/workflow/CounselResultVerifier.java) | 같은 테스트의 미확인 ID·예산 초과 거부 |
| 13-4 | [CounselService](src/main/java/com/example/ailearning/l13shopping/CounselService.java) | 같은 테스트의 조건 유지·실패·세션 격리·삭제 |

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:test --tests '*LocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:bootRun
```

로컬 테스트는 외부 모델을 호출하지 않습니다. [Live](src/test/java/com/example/ailearning/l13shopping/ShoppingAgentOpenAiLiveTest.java)와 앱은 루트 `.env`의 키·채팅 모델·임베딩 모델을 사용하며 시작 시 문서 임베딩부터 실제 API를 호출합니다. 포트는 8102이며 요청 예시는 교재 13-4에 있습니다. 종료는 Ctrl+C입니다.

전체 실행 제한·호출 추적은 별도 [13-8](../13-08-limited-agent/README.md)에 있습니다. 기본 앱 응답에는 trace가 없습니다.
