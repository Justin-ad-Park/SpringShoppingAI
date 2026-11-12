# 이름 기준 통합과 단원별 책임 정리 — 2026-11-12

## 범위와 보존한 결정

사용자 요청에 따라 기존 13장 변경을 `659b35d`로 먼저 커밋하고, 다른 단원의 샘플을 검토해 이름과 복잡한 처리만 개선했다. 실습 독립성, 기존 HTTP 경로·요청 이름·JSON 필드·도구 스키마·설정·상태 판정·메모리 저장 시점은 보존했다. 새 변경은 작업 트리에 남겼다.

작업 도중 13장의 `CounselRequestFacts` → `CounselToolResults` 및 관련 문서 편집이 별도로 추가된 것을 확인했다. 해당 변경을 덮어쓰지 않았으며 아래 02~12장 수정·검증 결과에 포함하지 않는다. 프로젝트 설정의 예시에는 실제 현재 이름을 사용했다.

## 하네스 기준 위치

- [이름 기준](../common/rules/naming.md): 식별자의 역할·검증 전후·수량·시간·외부 계약, 이름 검토 순서를 한 곳에서 관리한다. 기존 authoring의 상세 네이밍 설명은 이 문서로 옮겼다.
- [작성 기준](../common/rules/authoring.md#읽기-쉬운-이름과-코드-표현), [검토 질문](../common/rules/review.md#이름책임리팩터링--이름이나-구조를-바꿀-때), 수정·검증 절차와 단원 템플릿은 이름 기준을 참조한다.
- [프로젝트 설정](../project.md#이름과-패키지-배치): domain·workflow·tools·policy·support의 역할과 의존 방향, 자료형의 소유 위치, 작은 실습에 같은 패키지 세트를 강제하지 않는 기준을 둔다.
- 공통 버전은 0.2.23이다. 상세 이름 규칙과 과정별 패키지 예시를 구분했다.

## 코드 검토와 적용 결과

| 범위 | 확인한 문제와 적용 | 유지한 구조 |
|---|---|---|
| 02 다중 모델 | 제공 업체 이름만 있던 ChatClient 필드를 openAiChatClient·anthropicChatClient로 명확히 함 | 짧은 서비스와 제공 업체별 메서드 유지 |
| 04 구조화 응답 | extractCriteria·extractCriteriaAsText, CRITERIA_EXTRACTION_INSTRUCTION로 목적 표시 | HTTP 400·500·422를 학습하는 호출·변환·업무 검증 흐름 유지 |
| 05~06 도구·실행 방식 | 도구 객체·ChatClient 이름, runWorkflow·runAgent·runHybrid, 검증 후보와 프롬프트 상수 이름 개선 | 모델에 공개하는 searchProducts·calculateShipping과 인자 이름 유지. 방식별 짧은 메서드를 더 나누지 않음 |
| 07~08 메모리·Advisor | conversationMemory·conversationKey·requestMessages·advisorSteps·toolCallingAdvisorOrder로 자료와 순서의 대상 표시 | 07의 조회→질문 추가→호출→성공 저장 순서와 08의 Advisor 등록을 같은 메서드에서 읽도록 유지 |
| 09 검색 | compareSearchResults에서 키워드 검색과 벡터 검색을 각각의 메서드로 분리. SearchHit·SearchComparison·EmbeddingVector로 자료 역할 표시 | 객체·설정 계층을 추가하지 않음 |
| 10 RAG | policy에 PolicyLoader·PolicySearch·PolicySearchResult를 모음. RagService는 검색→requestGroundedAnswer→verifyCitations의 순서를 드러냄 | 생성 생략·답변 불가·출처 오류·오류 전파 정책 유지. Advisor 보충 실습은 이름만 정리 |
| 11 MCP | tools에 ShoppingMcpGateway·McpToolExecutor를 모음. callback 구성과 상한·인자 검사·실행·기록을 이름 있는 메서드로 구분 | callback 인터페이스, 원격 도구 이름·JSON 인자·실행 순서·상한 조건·검색 후 계산 무효화 시점 유지 |
| 12 평가 | RAG를 10장과 같은 역할 이름으로 정리하고 요청 계측·실패 분류·출처 검증을 구분. 평가 반복/집계와 문항별 비교를 분리 | 기존 채팅 시도·토큰·지연 측정 범위, 실패 분류·우선순위, 평가 실패 문구와 JSON 필드 유지 |
| 12 모델 판정·관측 | evaluateRelevance·evaluateFactualConsistency, 판정기·ChatClient의 역할 표시 | 작은 서비스의 직접 호출 구조 유지 |

00·01은 실행 코드가 없는 안내이며, 03은 첫 호출 모듈을 공유한다. 첫 호출, 재시도·타임아웃, 도구 상한 예제의 이미 명확한 구조와 이름은 유지했다. 새 디자인 패턴·상속 계층·공통 운영 모듈은 추가하지 않았다.

## 실행 검증

Java 21 / Gradle Wrapper 8.14.5 / Boot 4.0.9 / Spring AI 2.0.2. 버전의 기준은 루트 build.gradle과 실제 Gradle 출력이다.

아래 각 모듈의 `:test --tests '*LocalModelTest'`를 실행했다. 표시한 세 모듈에는 `--tests '*UnitTest'`를 함께 적용했다. 클래스 필터를 사용했으므로 Live는 실행하지 않았다. 11장은 로컬 MCP 서버를 실제 기동하고 모델은 HTTP stub을 사용한다.

| 모듈 | XML 테스트 수 | UnitTest 포함 |
|---|---:|---|
| 02-04-multiple-models | 3 | |
| 04-00-structured-output | 5 | |
| 05-00-tool-calling | 10 | 포함 |
| 06-00-workflow-agent | 10 | |
| 07-00-chat-memory | 7 | |
| 08-00-advisors | 5 | |
| 09-00-embeddings-search | 7 | |
| 10-00-rag | 6 | |
| 10-04-rag-advisor | 4 | 포함 |
| 11-00-mcp-workflow-agent | 15 | 포함 |
| 12-00-evaluation | 5 | |
| 12-02-model-evaluation | 4 | |
| 12-04-observability | 2 | |
| 합계 | 83 | 실패 0·오류 0·건너뜀 0 |

실행 명령 예시는 다음과 같다. 다른 행은 같은 모듈 경로 형식으로 실행했다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-00-rag:test --tests '*LocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:11-00-mcp-workflow-agent:test --tests '*LocalModelTest' --tests '*UnitTest'
```

결과는 각 모듈 build/test-results/test의 JUnit XML에서 집계했다. 실제 실행한 로컬 테스트 태스크 13개가 통과했으며, testClasses에 포함된 Live 코드도 컴파일했다. 의존성·선행 컴파일의 UP-TO-DATE 재사용을 실제 외부 호출로 해석하지 않는다. 선행 13장 커밋 직전의 로컬 통합 테스트 11개는 위 합계와 별도다.

## 문서·정적 검증과 한계

- 전체 파일로 제공한 Java 발췌 28개를 실제 소스와 대조해 차이 0개를 확인했다. 부분 발췌는 변경된 메서드·자료형에 맞춰 확인했다.
- 기존 HTTP 매핑 선언을 변경 전 소스와 비교해 변경 없음을 확인했다. 기존 통합 테스트로 JSON·상태·호출 순서도 확인했다.
- 현재 교재의 옛 식별자와 이동 전 경로를 검색해 사용처·링크를 갱신했다. 과거 보고서의 당시 이름은 유지하고 두 소스 링크만 현재 위치로 바꿨다.
- 변경한 09·10장 Mermaid 3개가 기존 임시 도구의 파서에서 통과했다. 화면 렌더링·실제 학습자의 이해도는 확인하지 않았다.
- 하네스·교재 로컬 링크와 앵커 834개 오류 0개, git diff --check 통과, 공통 VERSION/SOURCE의 0.2.23 일치를 확인했다.
- 교육·기술 검토는 한 에이전트가 관점을 바꾸어 수행했다. 독립 검토나 별도 세션 행동 평가를 수행한 것으로 기록하지 않는다.
- E09·E10·E12가 이름·절제된 분리·수량 의미 기준에 관련되지만 독립 세션 평가는 3개 중 0개 실행했다. 정적 규칙 연결과 현재 코드 테스트 통과가 향후 에이전트의 반복 행동을 보장하지는 않는다.
- 이번 후속 작업에서 유료 외부 모델 API·모델 품질 평가·bootJar 실행은 수행하지 않았다. 프롬프트에 표시되는 Java 타입 이름 변경의 실제 모델 영향은 미검증이다.
