# 커리큘럼과 기능별 실습 케이스

## 읽는 방법

00~02는 입문, 03~06은 대화·실시간 업무, 07~10은 검색·외부 연결, 11~13은 검증·통합 과정이다. 14는 선택 확장이다. 권장 선수 지식은 Java record, DI, REST, JUnit 기본이며, Flux·벡터·MCP 개념은 해당 단원에서 소개한다. 단원당 1~2회 실습을 기준으로 하되 RAG와 통합은 2~3회로 나눈다.

각 표의 메소드는 **예정 코드명**이다. 표의 모든 행에 대응하는 `@Test`를 만들고, 정상 조건과 실패·경계 조건을 모두 검증한다. 질문은 웹 기본 입력값으로도 사용한다. `FooCases#bar`의 테스트는 `FooCasesTest#bar_*`로 연결한다. 기본 검증은 결정적 stub이며, 실제 모델의 언어 이해나 생성 품질을 주장하는 항목만 별도 live 평가를 추가한다.

## 00. 준비: 모델 호출과 테스트의 경계

목표: 서버에서 모델을 호출하는 이유와 stub/live 차이를 이해한다. 산출물: 키 없는 기본 실행과 첫 테스트.

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L00-01 | SetupCases#helloStub | 쇼핑 도우미야, 안녕 | scripted 응답·요청 1회 확인; 빈 질문은 모델 호출 전 거부 |
| L00-02 | SetupCases#inspectMode | stub에서 live로 설정 변경 | 응답에 mode/model 표시; 키 없는 live는 설정 오류로 명확히 실패, stub 외부 호출 0회 |

## 01. ChatModel, ChatClient와 프롬프트

목표: Prompt/Message/ChatResponse와 fluent API를 연결한다. **알지 못하는 현재 상품 사실을 모델만으로 답할 수 없다는 점**을 먼저 체험한다. [ChatClient API](https://docs.spring.io/spring-ai/reference/api/chatclient.html)의 개념을 쇼핑 질문에 적용한다.

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L01-01 | ChatBasicsCases#compareModelAndClient | 선물 고를 때 고려할 점 3개 | ChatModel 직접 호출과 ChatClient 요청의 메시지 비교; null/빈 응답 처리 |
| L01-02 | ChatBasicsCases#separateRoles | 지금 텀블러 재고가 몇 개야? | system/user 분리, 재고 출처 미제공 표시; fixture 없는 숫자를 사실 카드로 만들지 않음 |
| L01-03 | PromptCases#renderTemplate | 예산 3만원, 동료 선물 추천 기준 | 템플릿 변수와 리소스 파일 확인; 누락 변수·중괄호 포함 사용자 입력 처리 |
| L01-04 | ChatBasicsCases#inspectOptionsAndUsage | 같은 질문을 두 설정으로 실행 | 전달된 옵션·provider usage 관찰; usage 미제공을 0으로 만들지 않음, 답변 동일성은 요구하지 않음 |

## 02. Structured Output과 업무 검증

목표: 자연어를 Java record로 변환하고, 파싱 성공과 업무 유효성이 다름을 익힌다.

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L02-01 | StructuredOutputCases#extractCriteria | 3만원 이하 생활용품 찾아줘 | SearchCriteria(keyword, category, maxPrice) 변환; 예산 음수·미지원 category 거부 |
| L02-02 | StructuredOutputCases#mapRecommendationList | 이 후보 목록을 비교해줘 | 제네릭 List<Recommendation> 매핑; 후보 밖 상품 ID·중복 ID 제거 또는 검증 실패 |
| L02-03 | StructuredOutputCases#handleInvalidOutput | 잘린 JSON/필드 누락 응답 주입 | 파싱 오류와 업무 검증 오류 구분; 제한된 교정 1회 후 종료, 무한 재질문 금지 |
| L02-04 | StructuredOutputCases#compareNativeSchema | 같은 record를 일반 변환/지원 모델 schema 모드로 요청 | native 지원 여부·스키마 요청 기록; 미지원은 명시적 capability 결과. 실제 모델 지원 검증은 live |

기본 변환과 native schema 옵션은 대상 모델·버전에서 확인한다. schema에 맞는 JSON도 상품 존재·가격·권한을 보증하지 않는다. 스트리밍 도중의 미완성 JSON을 업무 객체로 확정하지 않는다.

## 03. 스트리밍과 대화형 응답

목표: `.call()`과 `.stream()`의 차이, 취소·중간 실패를 학습한다.

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L03-01 | StreamingCases#streamBuyingGuide | 텀블러 고르는 기준 알려줘 | 순서 있는 delta와 최종 조립 텍스트 비교; 빈 chunk·한글 경계 처리 |
| L03-02 | StreamingCases#cancelResponse | 응답 중 정지 버튼 | 구독 취소가 upstream에 전달되고 완료 이벤트가 오지 않음; 공급자의 과금 즉시 중단까지 보장하지 않음 |
| L03-03 | StreamingCases#handlePartialFailure | 두 chunk 뒤 timeout 주입 | 부분 답변 표시 + error 1회, 성공 done 없음; 부분 메시지를 성공 메모리로 확정하지 않음 |

## 04. ChatMemory와 업무 상태

목표: “이전 대화 전달”과 “검증된 쇼핑 조건 유지”를 비교한다. 메모리 window는 최근 메시지 수를 제한하며 그 자체로 토큰 예산 제한을 보장하지 않는다.

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L04-01 | MemoryCases#compareNoMemoryAndMemory | 예산 3만원이야 → 그 예산으로 선물 고를게 | 두 번째 모델 요청에 이전 메시지가 있는지 비교; 단순 답변 문장 동일성 대신 전달 이력 검증 |
| L04-02 | MemoryCases#isolateAndReset | A는 음료, B는 생활 → A만 초기화 | 세션+case+conversation 격리; 동일 외부 conversationId를 쓴 다른 세션의 이력 조회·삭제 불가 |
| L04-03 | MemoryCases#windowMessages | 작은 window로 여러 턴 실행 | window 제거 규칙과 system 처리 확인; 큰 메시지는 별도 입력 제한 필요함을 테스트 |
| L04-04 | MemoryCases#persistWithJdbc | 저장 후 repository 재생성 | 같은 학습 DB에서 이력 복구, 초기화 반영; DB 장애가 빈 이력으로 은폐되지 않음 |
| L04-05 | MemoryCases#compareTaskState | 3만원 이하 음료 → 예산 해제 | 대화 원문과 SearchCriteria 상태 나란히 표시; KEEP/CLEAR 구분·숫자 검증은 Java 테스트 |

JDBC는 선택 인프라 케이스다. 핵심 ChatMemory는 메모리 저장소로 먼저 익힌다. 이력을 읽을 수 있다고 해서 모델이 모든 조건을 정확히 유지한다고 가정하지 않는다.

## 05. Advisor: 공통 전후 처리

목표: Advisor가 어느 단계에서 요청과 응답에 관여하는지 관찰한다.

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L05-01 | AdvisorCases#traceOrder | 같은 질문에 A/B advisor 순서 변경 | before/after의 실제 순서를 recording advisor로 검증; 동일 order에 기대지 않음 |
| L05-02 | AdvisorCases#maskDiagnostics | 전화번호가 포함된 문의 | 학습 진단 출력에서 마스킹; 원문 로깅 비활성. 마스킹만으로 provider 전송까지 막았다고 주장하지 않음 |
| L05-03 | AdvisorCases#composeMemoryAndTrace | 후속 질문 2회 | trace와 memory 조합, context 전달 범위 확인; 중복 저장·다른 case advisor 혼입 없음 |

TraceAdvisor 같은 이름은 프로젝트가 구현할 교육용 코드다. Spring AI 내장 advisor와 구분한다. 프롬프트 거절만으로 접근 제어를 구현하지 않는다.

## 06. Tool Calling: 상품·장바구니·배송

목표: 모델 요청 → Java 도구 실행 → 결과 재전달 → 설명 생성 흐름을 익힌다. 로컬 도구부터 시작한다. Spring AI의 `@Tool`, `@ToolParam`, ToolContext와 도구 콜백을 사용하되 Java 업무 검증을 유지한다. [공식 도구 실행 흐름](https://docs.spring.io/spring-ai/reference/api/tools.html)

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L06-01 | ToolCallingCases#searchProducts | 1만5천원 이하 음료 찾아줘 | schema·도구 인자·재고·가격 경계 검증; 품절/음수/미지원 category 처리 |
| L06-02 | ToolCallingCases#quoteFreeShipping | 무료배송까지 얼마나 부족해? | cart+policy+quote 결과, 42,000→부족 8,000원; 50,000 경계·빈 장바구니·파트너 배송 예외 |
| L06-03 | ToolCallingCases#passTrustedContext | 내 장바구니 확인해줘 | 세션에서 만든 고객 식별자를 ToolContext로 전달; 모델이 타 고객 ID를 넣어도 접근 불가 |
| L06-04 | ToolCallingCases#boundToolExecution | 잘못된 도구명/반복 호출 응답 주입 | allowlist, timeout, 턴당 8회 실행 상한; 오류를 검색 결과 없음과 구분 |
| L06-05 | ToolCallingCases#confirmSandboxCart | P004 하나 담아줘 → 확인 | preview 후 확인 토큰으로 샌드박스만 변경; 미확인 0회, 토큰 소유권·만료·중복 제출 검증 |

L06-05는 후반 선택 실습이다. 기존 MCP에는 실제 장바구니 변경 기능이 없으므로 Tutor 전용 fake 도구를 만든다. 재시도 때문에 변경이 중복되지 않도록 commandId를 사용한다. live 모델이 적절한 도구를 고르는지는 별도 평가한다.

## 07. Embedding과 VectorStore 기초

목표: 생성 모델과 임베딩 모델의 역할, 벡터 검색과 정확한 상품 필터의 차이를 배운다.

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L07-01 | EmbeddingCases#compareSimilarity | 보온용 컵 / 따뜻하게 마시는 텀블러 / 배송 문의 | 결정적 벡터로 cosine 계산 검증; 실제 문장 순위는 live 측정, 고정 사실처럼 단정하지 않음 |
| L07-02 | VectorStoreCases#indexAndSearch | 따뜻한 차 마실 용품 | add/search/delete·topK, 문서 ID 확인; 없는 ID 삭제·빈 검색 처리 |
| L07-03 | VectorStoreCases#filterMetadata | 생활 카테고리 중 재고 있는 것 | category/inStock 필터, 결과 없는 조합; 가격/재고는 원본 상품으로 재검증 |
| L07-04 | VectorStoreCases#swapStore | 메모리 저장소 → pgvector | 동일 계약으로 결과 필드·필터 검증; 모델/차원 불일치 시 재색인 필요, 서로 다른 저장소 점수 동일성 요구 안 함 |

처음에는 소규모 메모리 저장소를 사용한다. pgvector는 선택 실습이다. 임베딩 모델·차원·데이터 버전을 색인 manifest에 기록한다.

## 08. ETL: 정책 문서를 검색 가능한 데이터로

목표: reader → transformer → writer 경계를 눈으로 확인한다.

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L08-01 | EtlCases#readPolicyDocuments | 가상 배송/반품 정책 TXT·JSON 읽기 | 문서 내용·source/version 메타데이터; 빈 파일·누락 리소스 실패 |
| L08-02 | EtlCases#splitAndAttachMetadata | 긴 정책을 여러 chunk로 분할 | 원문 추적 ID, 분할 경계, 메타데이터 유지; 내용 유실·빈 chunk 검사 |
| L08-03 | EtlCases#reindexIdempotently | 같은 정책 재적재 → 새 버전 교체 | 문서+버전+chunk의 안정 ID, 중복 없음; 이전 버전 제거, 실패 시 새 색인 활성화 금지 |
| L08-04 | EtlCases#readPdfAndHtml | 가상 설명서 PDF/HTML 비교 | 텍스트·페이지/출처 추적, 표와 이미지 추출 한계; 텍스트 없는 PDF는 OCR 미지원 안내 |

reader와 splitter의 정확한 지원 타입·옵션은 선택 버전에 맞춘다. `TokenTextSplitter`의 숫자 인자를 임의로 overlap이라 해석하지 않고 해당 버전 API 의미를 확인한다. PDF/HTML은 로컬 고정 문서로 실행하며 실시간 크롤링은 필수 과정이 아니다.

## 09. RAG: 근거가 있는 정책 답변

목표: 검색한 정책과 현재 상품 사실을 구분하며 “모른다”를 정상 결과로 다룬다.

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L09-01 | RagCases#answerWithManualContext | 미개봉 상품 반품 조건은? | 검색→컨텍스트 조립→답변을 명시적으로 작성; source ID 없는 인용·검색 밖 정책 주장 검출 |
| L09-02 | RagCases#answerWithQaAdvisor | 같은 질문을 advisor 방식으로 | QuestionAnswerAdvisor와 수동 흐름 비교; 검색 설정·검색 문서 전달 확인 |
| L09-03 | RagCases#handleMissingEvidence | 해외 배송 관세는? | 문서 없음 상태와 근거 부족 응답; 모델 호출 생략 정책을 Java로 검증 |
| L09-04 | RagCases#filterPolicyAndTreatUntrustedText | 이전 정책/다른 몰 문서/지시문 삽입 | 서버가 current/store 필터 적용; 문서 속 지시를 권한으로 사용하지 않음, 가짜 인용 거부 |
| L09-05 | RagCases#improveRetrieval | 그럼 뜯은 건? | 질문 재작성·topK 비교, 선택적으로 RetrievalAugmentationAdvisor 구성; 원 질문 보존·이력 격리·검색 재현 평가 |

L09-05는 심화다. 지원 API가 2.0.2에 없으면 명시적 Java 파이프라인으로 원리를 먼저 학습한다. 검색 점수 임계값은 데이터셋으로 조정하며 유사도 점수를 사실 확률로 표시하지 않는다. 답변 출처는 실제 검색 문서 ID에서만 연결한다.

## 10. MCP Client/Server 연결

목표: 로컬 `@Tool`과 원격 MCP 도구 호출을 비교한다. MCP 클라이언트는 Tutor 애플리케이션에 있고, 모델 자체가 HTTP로 MCP 서버에 접속하는 것이 아니다.

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L10-01 | McpCases#discoverAndCall | 원격 상품 검색 도구 보기/직접 실행 | 기존 shopping-mcp의 discovery·schema·실제 호출 확인; 연결 실패를 unavailable로 표시 |
| L10-02 | McpCases#letModelUseRemoteTool | 1만5천원 이하 음료 추천해줘 | MCP 도구를 ChatClient에 연결한 요청/결과 추적; 허용 도구만 노출, 모델 반복 상한 |
| L10-03 | McpCases#compareLocalAndRemote | 무료배송 견적을 로컬/원격으로 | 동일 fixture에서 사실 결과 비교; transport 오류·timeout과 업무 결과 없음 구분 |
| L10-04 | McpCases#exposeTutorPolicyTool | Tutor의 가상 정책 조회를 서버로 노출 | 선택 별도 테스트 서버로 등록→발견→호출; 중복 도구명·schema 오류 검사 |

기본 연결은 기존 MCP의 Streamable HTTP 프로토콜에 맞춘다. STDIO는 별도 비교 과제다. 모든 전송 방식을 한 번에 필수로 만들지 않는다. L10-04용 서버는 기본 Tutor 실행에 포함하지 않는 선택 확장이며 기존 shopping-mcp를 수정할 필요가 없다. MCP 프로토콜 세션을 로그인 사용자 ID로 사용하지 않는다.

## 11. Evaluation: 무엇을 잘한다고 판단할까

목표: stub 테스트와 실제 모델 평가의 증거 범위를 구분한다. Spring AI Evaluator는 보조 평가 도구로 활용한다. [Evaluation Testing](https://docs.spring.io/spring-ai/reference/api/testing.html)

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L11-01 | EvaluationCases#checkBusinessFacts | 추천 카드·배송 합계 검사 | 상품 ID·가격·재고·배송비·출처의 결정적 검증, 조작된 답변은 실패 |
| L11-02 | EvaluationCases#judgeRelevance | 근거와 관계있는/없는 답변 비교 | RelevancyEvaluator 입력과 결과 기록; 판정 모델 오류·불완전 응답은 평가 실패로 분리 |
| L11-03 | EvaluationCases#runRegressionDataset | 질문 세트로 설정 A/B 비교 | 의미 조건 충족률·검색 recall·오류율 보고; judge 통과로 업무 검증 실패를 덮지 않음 |

## 12. 관측, 모델 교체와 장애 처리

목표: 모델 사용량과 지연, 애플리케이션 실패 경계를 이해한다. 관측은 Actuator/Micrometer와 Spring AI의 관측 지점을 활용하되 지원 항목을 버전에서 확인한다.

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L12-01 | OperationsCases#recordUsageAndLatency | 동일 질문 여러 번 실행 | model/토큰/전체·도구 지연 기록, usage 누락 허용; 원문/세션 ID를 metric label로 사용하지 않음 |
| L12-02 | OperationsCases#retryTransientRead | 429/503/timeout/401 응답 주입 | 읽기 요청의 제한된 재시도·최대 시간, 401 재시도 없음; 변경 작업 자동 재실행 없음 |
| L12-03 | OperationsCases#switchCompatibleModel | 같은 질문을 두 provider에서 실행 | 공통 입력 계약·기능 지원 차이 노출; 도구/schema 미지원은 설정 단계에서 안내 |

모델 교체는 모든 기능이 설정만으로 동일하게 동작함을 뜻하지 않는다. chat/embedding/image/audio는 별도 모델 설정이며 tool·native schema·vision 지원을 capability 표로 관리한다.

## 13. 통합: 검증 가능한 쇼핑 어시스턴트

목표: 앞서 읽은 메소드들을 명시적인 Java 흐름으로 조합한다. 자유로운 다중 에이전트 계획은 필수 범위에 포함하지 않는다.

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L13-01 | ShoppingAssistantCases#recommendWithFollowUp | 3만원 이하 선물 → 음료만 → 예산 해제 | 구조화 조건·메모리·상품 도구 결합; 조건 유지/해제, 검증된 카드, 세션 격리 |
| L13-02 | ShoppingAssistantCases#recommendFreeShipping | 무료배송 채워줘 → 음료로만 | 8,000~15,000원 기본 범위, P004/P005의 개별 견적; 이미 무료/빈/무결과/품절/파트너 처리 |
| L13-03 | ShoppingAssistantCases#combinePolicyAndCatalog | 텀블러 추천하고 반품 조건도 알려줘 | 상품 사실은 도구, 정책은 RAG; 한쪽 장애를 다른 쪽 성공으로 위장하지 않음 |
| L13-04 | ShoppingAssistantCases#recoverAndReset | 검색 후 timeout → 재질문 → 초기화 | 실패 턴이 확정 상태를 손상하지 않음; 새 대화의 메모리·조건·샌드박스 초기화 |

기본 무료배송 추천 상한 `부족액+7,000원`은 기존 PoC와 비교하기 위한 Tutor 정책이다. 사용자 명시 예산을 우선하고 정책 자체를 Spring AI 기능이라고 설명하지 않는다. 검색/계산은 Java, 설명은 모델, 정책 질의는 문서 검색이라는 역할 분담을 설명할 수 있어야 수료한다.

## 14. 선택: 멀티모달과 추가 모델 API

목표: Spring AI에서 제공하는 이미지 입력/생성·음성·moderation의 적용 범위를 체험한다. provider별 지원·추가 비용·선택 버전 의존성을 확인한 뒤 구현한다. [공식 기능 목록](https://docs.spring.io/spring-ai/reference/)

| ID | 클래스#메소드 | 질문/실험 | 관찰과 자동화 검증 |
|---|---|---|---|
| L14-01 | MultimodalCases#searchFromProductImage | 이 컵과 비슷한 상품 있어? | 이미지 특징 추출 후 실제 상품 검색; MIME/크기/비이미지 거부, 이미지로 가격·재고 단정 금지 |
| L14-02 | MultimodalCases#transcribeShoppingRequest | 녹음: 3만원 이하 텀블러 찾아줘 | transcription을 수정 가능한 입력으로 표시; 빈/손상 음성 거부, 전사만으로 구매 실행 없음 |
| L14-03 | MultimodalCases#speakRecommendation | 이 추천 설명 읽어줘 | TTS bytes와 content type, 텍스트 길이 제한·실패 응답; stub는 오디오 계약만 검증 |
| L14-04 | MultimodalCases#generateGiftPreview | 가상 선물 포장 이미지 만들어줘 | ImageModel 결과를 생성 이미지로 표시; 실제 상품 사진·판매 보장으로 사용 안 함, 오류 처리 |
| L14-05 | ModerationCases#checkAbusiveMessage | 모욕성 입력/일반 문의 비교 | 지원 provider의 moderation 결과를 업무 정책에 연결; timeout 시 결과 미확정 표시, 업무 권한과 별개 |

텍스트 대화의 실시간 오디오 전송, OCR 전용 서비스, 이미지 모델 훈련, 결제·실제 주문, 운영 인증/결제 시스템 구현은 이번 커리큘럼 범위 밖이다.
