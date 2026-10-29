# 학습 코드 안내

06 구조 개선안을 적용했다. 58개 caseId와 REST/SSE API를 유지하고 Java 예제와 학습실 실행 기반을 분리했다.

처음 배우는 경우 [짧은 코드로 시작하는 다섯 수업](09-first-shopping-lessons.md)을 먼저 보세요.

## 시작 위치

- [00 ChatClient 첫 호출과 STUB/LIVE 경계](../src/main/java/com/example/shopping/tutor/lesson/l00setup/README.md)
- [01 ChatModel·ChatClient·역할·프롬프트·옵션](../src/main/java/com/example/shopping/tutor/lesson/l01chat/README.md)
- [02 JSON 응답을 검증된 record로 변환](../src/main/java/com/example/shopping/tutor/lesson/l02structured/README.md)
- [03 Flux 구독·취소·부분 실패](../src/main/java/com/example/shopping/tutor/lesson/l03streaming/README.md)
- [04 대화 기억·window·JDBC·업무 상태](../src/main/java/com/example/shopping/tutor/lesson/l04memory/README.md)
- [05 Advisor 순서와 메모리 조합](../src/main/java/com/example/shopping/tutor/lesson/l05advisors/README.md)
- [06 모델의 도구 선택과 Java 실행](../src/main/java/com/example/shopping/tutor/lesson/l06tools/README.md)
- [07 임베딩·cosine·벡터 검색·필터](../src/main/java/com/example/shopping/tutor/lesson/l07vector/README.md)
- [08 문서 reader·청크·메타데이터·재색인](../src/main/java/com/example/shopping/tutor/lesson/l08etl/README.md)
- [09 검색·컨텍스트·답변·출처·질문 재작성](../src/main/java/com/example/shopping/tutor/lesson/l09rag/README.md)
- [10 MCP discovery·호출·모델 연계·서버 노출](../src/main/java/com/example/shopping/tutor/lesson/l10mcp/README.md)
- [11 결정적인 사실 검증·관련성 평가·회귀 질문](../src/main/java/com/example/shopping/tutor/lesson/l11evaluation/README.md)
- [12 usage·지연·제한된 읽기 재시도](../src/main/java/com/example/shopping/tutor/lesson/l12operations/README.md)
- [13 검색 조건·상품 사실·배송 계산·RAG 통합](../src/main/java/com/example/shopping/tutor/lesson/l13shopping/README.md)
- [14 이미지 이해·전사·음성 합성·이미지 생성·moderation](../src/main/java/com/example/shopping/tutor/lesson/l14multimodal/README.md)

각 README에 목표, 선수 단원, 핵심/support/테스트 파일, caseId, 질문과 실패 실험이 있다. 웹의 소스 보기에서도 같은 단원의 안내·핵심·support·테스트를 읽는 순서대로 선택할 수 있다.

## 코드 책임

- lesson 단원 루트: Spring AI API 호출, record, Tool, Advisor, 모델별 설정.
- 단원 support: 가상 원문·상품·장바구니, DB 준비, 표시 자료. 각 단원 README에서 용도를 설명한다.
- platform.config: 키·URL·실행 모드와 HTTP 클라이언트 조립.
- platform.web.adapter: 기존 58개 caseId와 학습 메소드 연결. 학습 결과를 웹 Outcome으로 바꾸고 성공한 검색 조건을 staging에 반영한다.
- platform.execution: LessonInput 입력 검증, LessonExecution의 턴별 상태와 성공 확정, BoundedToolCallbacks 실행 상한.
- platform.observation: ObservedChatModel 요청 관찰과 RunDiagnostics usage 기록.
- platform.stub: ScriptedChatModel의 준비된 응답.

BasicChatLesson은 ChatModel을 생성자로 받고 String/Comparison/ChatResponse를 반환한다. ConversationMemoryLesson은 모델·메모리를 받고 질문과 대화 ID로 호출한다. 다른 단원도 LessonExecution이나 웹 Outcome을 받지 않는다. 여러 관찰 항목을 비교하는 수업의 Result는 해당 단원에 한정한 반환 record다.

## 기존 파일의 대체 위치

| 이전 파일 | 현재 책임별 위치 |
|---|---|
| RunContext | platform.execution 및 observation |
| Shop | 02 SearchCriteria/Recommendation, 06 ProductSearchTools/CartSummaryTools 및 각각의 작은 support |
| Knowledge | 09 PolicyDocumentFixtures/DocumentDisplay, PolicyVectorIndex와 07 VectorStoreLesson의 실제 저장소 호출 |
| MediaModels | 14 ImageModelConfiguration/AudioModelConfiguration/ModerationModelConfiguration, 키 조립은 platform.config.MediaModelConfiguration |
| CoreBehaviorTest | lesson/l00setup~l14multimodal의 단원 테스트 |

공유 상품 fixture는 06, 공유 가상 정책 fixture는 09에 원본을 두며, 사용하는 단원 README에서 경로를 명시한다. 13 통합 수업은 02 조건 추출·04 병합·06 상품 사실·09 RAG를 명시적으로 재사용한다.

## 실행과 검증

```bash
./Spring-AI-Tutor/scripts/test_stub.sh
./Spring-AI-Tutor/scripts/test_mcp.sh
./Spring-AI-Tutor/scripts/test_live.sh
./Spring-AI-Tutor/scripts/test_live_mcp.sh
```

LIVE 두 스크립트는 기존 .env의 실제 키를 사용하므로 API 사용량이 발생한다. 기본·로컬 provider 계약·웹 테스트는 키가 필요 없다. 테스트 소스는 학습용 Java 파일만 JAR에 넣으며, 소스 API는 해당 단원에 등록된 파일만 제공한다.

## 적용 후 검증 (2026-10-29)

- 클린 빌드 후 기본·단원·provider 계약·웹 API 테스트: 175개 통과.
- 실제 MCP 서버 및 Tutor 정책 서버 테스트: 5개 통과.
- LIVE 모델·도구·임베딩/RAG·스트림 회귀: 6개 통과.
- LIVE 모델의 실제 원격 MCP 도구 선택: 1개 통과.
- JAR 빌드, JavaScript 문법 검사, 브라우저의 핵심/테스트 파일 탐색 확인.

pgvector 전용 DB와 실제 유료 멀티모달 생성은 이번 구조 변경 검증에 포함하지 않았다. 해당 경로의 로컬 provider 계약 테스트는 기본 테스트에 포함한다.
