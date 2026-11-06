# 06~13장 학습 목표와 예제 구성 검토

## 요청과 확인 범위

교육 목적을 먼저 정하고 예제가 그 목표에 맞는지 검토한다. 00~05는 이번 재검토와 개선 목록에서 제외했다. 06~13의 목표·목차와 관련 실행 코드를 정적으로 대조했다. 런타임 동작이나 모델 응답 품질 검증 결과를 뜻하지 않는다.

## 반영 여부와 이번 수정

- 06-1의 목표와 부모 목차에는 “검색·배송비 순서가 Java 코드임을 찾습니다”가 그대로 남아 있었다. 모델의 조건 추출과 상품 검색·배송비 계산·예산 검증·답변 생성을 정해진 순서로 연결하는 Workflow 구현을 목표로 수정했다. 확인 질문과 교육 계획도 맞췄다.
- 06-2는 프롬프트 비교에서 모델이 필요한 도구·인자를 선택하는 Agent 구현으로, 06-3은 막연한 책임 구분에서 모델의 조회 선택과 코드의 금액·카테고리 검증을 결합하는 혼합 방식 구현으로 목표를 구체화했다.
- 08-3은 로그 관찰 자체보다 Advisor 위치에 따른 실행 순서·반복 횟수와 중복 저장 확인을 목표로 수정했다.
- 11-2는 “모델이 반드시 필요한 것은 아님”이라는 부정형 목표 대신 MCP 도구 목록 조회와 지정 도구 호출을 목표로 수정했다.
- 13-1은 클래스 위치 찾기 대신 상품 추천·정책 안내·후속 질문을 처리할 책임과 흐름 설계를 목표로 수정했다.
- 각 부모 목차의 완료 기준을 함께 갱신했다. 실행 코드·설정·테스트는 수정하지 않았다. 아래 항목은 후속 코드 개선 목록이며, 구현 완료 항목이 아니다.

## 후속 적용 상태

이 문서의 목록은 코드 변경 전 검토 기록이다. 이후 사용자 요청으로 06의 기본 흐름과 자체 제약 구현을 분리하고, 확장 1·2를 모두 06장에 독립 실행 모듈로 구현했다. 12장에서는 같은 실습을 재활용한다. 아래의 초기 권고 위치인 “08 이후/12장 별도 실습”은 이 후속 요청으로 변경되었다. 07·08·10·11·12의 나머지 코드 구조와 13의 자체 제약 구현은 이번 06장 변경에서 리팩터링하지 않았다.

검증과 완료 범위는 [확장 실습 적용 기록](2026-11-06-execution-extensions.md)에 정리한다.

## 코드·설명 개선 목록

| 중요도 | 위치 | 문제와 학습에 미치는 영향 | 개선 방향 |
|---|---|---|---|
| 높음 | [06-1](../../Spring-AI-Agent-Learning/docs/lessons/06-workflow-agent/01-workflow.md), [ComparisonService](../../Spring-AI-Agent-Learning/labs/06-workflow-agent/src/main/java/com/example/ailearning/l06decisions/ComparisonService.java)의 `run()`, `client()`와 당시의 06-4 Future·06-5 자체 제한 설명(현재 [확장 1](../../Spring-AI-Agent-Learning/docs/lessons/06-workflow-agent/04-tool-call-limits.md)·[확장 2](../../Spring-AI-Agent-Learning/docs/lessons/06-workflow-agent/05-model-timeout-retry.md)로 대체) | 순서를 연결하는 수업인데 실제 실행에는 자체 횟수 계수, 마감 시간, 별도 스레드, Future, 추적까지 필요하다. 보조 코드를 읽지 말라는 안내만으로 의존성이 없어지지 않는다. | 기본 Workflow를 제약 구현 없이 독립 실행하도록 단순화한다. 도구 제한은 Spring AI 기본 기능을 사용하는 08 이후 확장 실습, 모델 요청 타임아웃·재시도는 12장의 별도 실습으로 분리한다. 전체 실행 시간·모델 호출 예산은 선택 심화로 보류한다. |
| 높음 | [06-2](../../Spring-AI-Agent-Learning/docs/lessons/06-workflow-agent/02-agent.md), `ComparisonService.agent()`와 당시 TracedShoppingTools(현재 [ShoppingTools](../../Spring-AI-Agent-Learning/labs/06-workflow-agent/src/main/java/com/example/ailearning/l06decisions/ShoppingTools.java)와 BudgetShoppingTools로 분리) | 기본 Agent 메서드가 다음 수업의 `fixed` 조건과 자체 RunState에 의존한다. 목표·도구 제공이라는 핵심을 배우기 전에 혼합 방식의 인자 보정과 기록 정책을 만나게 된다. | 기본 Agent는 목표와 도구를 제공하는 흐름만으로 실행되게 한다. 고정 조건과 조회 사실을 이용한 최종 검증은 혼합 방식에서 추가한다. 예산 초과 제외·결과 없음 처리는 업무 규칙이므로 유지한다. |
| 중간 | [07-2](../../Spring-AI-Agent-Learning/docs/lessons/07-chat-memory/02-store.md), [MemoryChatService](../../Spring-AI-Agent-Learning/labs/07-chat-memory/src/main/java/com/example/ailearning/l07memory/MemoryChatService.java) | 조회→프롬프트 전달→저장이 핵심인데 첫 전체 코드에 소유자 복합 키, 전체 요청 직렬화, 메시지 표시용 변환이 함께 등장한다. 핵심 메모리 흐름과 격리 정책을 구분하기 어렵다. | 기본 대화 조회·전달·저장이 실행되는 예제를 먼저 제시하고, 소유자 격리·삭제·표시 정보는 07-3에서 단계적으로 추가한다. 기존 여러 사용자 API에서 격리 검사를 단순 삭제하는 방식은 피한다. |
| 중간 | [08-2](../../Spring-AI-Agent-Learning/docs/lessons/08-advisors/02-memory-loop.md), [AdvisorChatService](../../Spring-AI-Agent-Learning/labs/08-advisors/src/main/java/com/example/ailearning/l08advisors/AdvisorChatService.java) | 메모리 자동 연결과 도구 루프 안팎의 A/B 계측을 한 메서드에서 함께 도입한다. 앞 수업의 단순 Advisor 체인에서 복수 개념으로 바로 넘어간다. | 메모리 Advisor로 이전 대화를 전달하는 실행 흐름을 먼저 완성하고, 도구 루프 위치에 따른 계측 실험을 다음 단계로 분리한다. 직접 저장과 Advisor 저장의 실패 시점 차이는 중요한 동작이므로 설명을 유지한다. |
| 중간 | [10-1](../../Spring-AI-Agent-Learning/docs/lessons/10-rag/01-indexing.md), [PolicyLoader](../../Spring-AI-Agent-Learning/labs/10-rag/src/main/java/com/example/ailearning/l10rag/PolicyLoader.java), [RagService 생성자](../../Spring-AI-Agent-Learning/labs/10-rag/src/main/java/com/example/ailearning/l10rag/RagService.java) | 적재와 검색 시점을 구분하는 목표에 비해 정규식 기반 Markdown 파서 설명이 대부분이다. 그림에는 벡터 저장이 있지만 본문 코드에는 실제 `store.add(...)` 연결이 없다. | 문서 준비→임베딩·저장→질문 시 검색이라는 실제 연결을 먼저 보여준다. 파서 세부 구현은 보충으로 옮기고, 저장을 수행하는 생성자 코드와 실행 결과를 연결한다. 목표를 파일 파싱 학습으로 바꾸지는 않는다. |
| 높음 | [11-4](../../Spring-AI-Agent-Learning/docs/lessons/11-mcp-workflow-agent/04-agent.md), [McpAgentService](../../Spring-AI-Agent-Learning/labs/11-mcp-workflow-agent/src/main/java/com/example/ailearning/l11mcp/McpAgentService.java), [McpToolRun](../../Spring-AI-Agent-Learning/labs/11-mcp-workflow-agent/src/main/java/com/example/ailearning/l11mcp/McpToolRun.java) | MCP 도구를 모델에 제공하는 기본 Agent에도 자체 호출 제한, 기록용 callback, 뒤에서 배울 혼합 방식의 조건 제한이 한 보조 객체로 연결된다. 06과 같은 문제가 반복된다. | MCP 연결과 `.toolCallbacks(...)`로 동작하는 기본 예제를 먼저 완성한다. 호출 제약은 확장으로, 조건 강제와 검색 결과 전체 ID 검증은 11-5의 업무 정책으로 분리한다. 허용 도구 제한과 금액 검증은 유지한다. |
| 중간 | [12-3](../../Spring-AI-Agent-Learning/docs/lessons/12-evaluation/03-usage.md), [RagService](../../Spring-AI-Agent-Learning/labs/12-evaluation/src/main/java/com/example/ailearning/l12evaluation/RagService.java)의 `Stats`, `answer()`, `answerInternal()` | 응답의 토큰 사용량을 읽는 목표에 비해 전체 실행 예제는 자체 시간 측정·호출 계수·예외 원인 순회·평가용 결과 포장을 함께 요구한다. usage를 읽는 핵심과 운영용 집계의 경계가 흐려진다. | 객체와 usage를 함께 받는 한 번의 호출을 실행 가능한 형태로 먼저 제공한다. 누적 관측은 12-4, 실패 분류는 12-5에서 연결한다. SDK 재시도를 포함한 HTTP 횟수와 서비스 계수의 차이도 구분한다. |
| 높음 | [13-1](../../Spring-AI-Agent-Learning/docs/lessons/13-shopping-agent/01-map.md), [13-4](../../Spring-AI-Agent-Learning/docs/lessons/13-shopping-agent/04-conversation.md), [CounselService](../../Spring-AI-Agent-Learning/labs/13-shopping-agent/src/main/java/com/example/ailearning/l13shopping/CounselService.java)의 `chat()` | 최종 통합에서도 자체 RunState·CountedChatModel·Future가 필수이며 이를 06 복습으로 안내한다. 06의 기본 예제를 단순화하면 해당 복습 관계도 맞지 않는다. | 상품·정책·메모리·결과 검증을 연결하는 기본 통합 흐름을 제약 구현과 분리한다. 기본 과정의 의존 관계와 복습 안내를 함께 갱신한다. 실제 조회 사실을 보관하는 TurnFacts와 가격·출처 검증은 통합 요구사항이므로 유지한다. |

우선 적용 순서는 06의 기본/확장 분리 → 같은 의존성이 있는 11·13 → 07·08·10·12의 단계별 설명과 실행 예제 정리다. 단순히 목표 문장만 넓혀 복잡한 코드를 정당화하지 않는다.

09장의 임베딩·저장·검색 실험과 12-4의 기본 지표 실습은 목표와 핵심 코드의 관계가 비교적 명확해 이 목록에 넣지 않았다. 모든 관찰·비교 수업을 구현 수업으로 바꿀 필요는 없다.

## 하네스 반영 목록

1. `common/rules/authoring.md`의 기존 목표 기준: 해결할 문제와 학습자의 수행 능력을 먼저 정하고 예제를 선택하도록 보강했다. 코드·클래스 위치 찾기를 목표로 대신하지 않도록 명시했다.
2. 같은 절의 기존 정합성 기준: 목표와 맞지 않는 코드에는 설명을 늘리기 전에 축소·단계 분리·선택 확장을 검토하도록 보강했다. 이는 모든 오류 처리나 업무 검증을 제거하라는 뜻이 아니다.
3. `common/rules/review.md`의 기존 첫 질문을 위 기준에 맞췄다. 별도 절차·템플릿·평가 사례·중복 체크리스트는 추가하지 않았다.

공통 버전은 0.2.15로 갱신했다. 기술별 모듈 분리 위치는 위 과정별 개선 목록에서 관리하며 범용 규칙에 Spring AI 단원 번호를 넣지 않는다.

## 검증 범위

- 문서 목표·부모 목차·교육 계획의 대응과 실제 소스의 의존 관계를 확인했다.
- 하네스·교재 문서의 로컬 링크·앵커 656개를 검사했으며 오류는 0개였다. `git diff --check`도 통과했다.
- 실행 코드·설정·테스트는 변경하지 않았으므로 Gradle·Live 테스트를 실행하지 않았다.
- Mermaid 코드는 변경하지 않았다.
- 하네스의 에이전트 행동 평가는 실행하지 않았다. 규칙 수정의 정적 검토와 행동 평가를 구분한다.
