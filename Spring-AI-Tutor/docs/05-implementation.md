# 구현 현황과 설계안의 구체화

기준일: 2026-10-29. 초기 01~04 문서는 설계 배경과 확장 방향을 포함한다. **현재 실행 명령·기능 상태는 프로젝트 [README](../README.md)와 실제 소스가 기준**이다.

## 실제 구성

```text
Spring-AI-Tutor/
  build.gradle
  .env.example
  scripts/                   # env.sh, run.sh, stop.sh, pgvector-up.sh
    test_stub.sh             # 공통 실행기에 --stub 전달
    test_live.sh             # 공통 실행기에 --live 전달
    test_mcp.sh              # 공통 실행기에 --mcp 전달
    test_live_mcp.sh         # 공통 실행기에 --live-mcp 전달
    test/test.sh             # 테스트 공통 실행기
  src/main/java/com/example/shopping/tutor/
    TutorApplication.java
    lesson/l00setup~l14multimodal/  # 단원별 학습 코드, README, 필요한 support
    platform/config/              # 키·URL·모드 조립
    platform/web/adapter/         # 58개 caseId를 학습 메소드에 연결
    platform/execution/           # 입력 검증·턴별 상태·실행 상한
    platform/observation/         # 모델 요청·usage 관찰
    platform/stub/                # 준비된 응답 모델
  src/main/resources/
    application.yml
    knowledge/               # 로컬 가상 정책 TXT/JSON
    static/tutor/            # 독립 웹 학습실
  src/test/java/com/example/shopping/tutor/
    lesson/l00setup~l14multimodal/  # 단원 직접 테스트와 관련 계약·LIVE·MCP 테스트
    platform/web/                 # 전체 catalog·세션·REST/SSE 회귀
    platform/stub/                # 로컬 provider 계약 테스트
    platform/support/             # 테스트 실행 fixture

```

단원별 핵심 코드와 준비 자료는 같은 폴더에 모았다. 웹 연결 annotation은 platform.web.adapter에만 있고 학습 메소드는 웹 실행 객체를 받지 않는다. 구체적인 읽는 순서는 [학습 코드 안내](08-learning-code-guide.md)를 참고한다.

## 실행 케이스 상태

| 단원 | 구현 상태 |
|---|---|
| 00~03 | 실제 ChatModel/ChatClient, PromptTemplate, entity, native schema 옵션, Flux 스트림 |
| 04~05 | MessageChatMemoryAdvisor, MessageWindowChatMemory, JDBC H2 repository 복구, Advisor 순서·진단 마스킹 |
| 06 | 실제 ToolCallingManager/callback 경로, ToolContext, 상한 8회, 확인 토큰·멱등 샌드박스 |
| 07~08 | EmbeddingModel, SimpleVectorStore, 선택 PgVectorStore, TXT/JSON/PDF/HTML reader, TokenTextSplitter |
| 09 | 수동 RAG, QuestionAnswerAdvisor, 서버 소유 메타데이터 필터, 근거 없음, 후속 검색어 재작성 |
| 10 | 실제 MCP SDK의 Streamable HTTP discovery/호출, 선택 Tutor 정책 서버 |
| 11~12 | RelevancyEvaluator, Java 사실 검증, 조건 회귀셋, usage·지연, 제한된 장애 실험, 실행 모델 비교 |
| 13 | 조건 병합·상품 검색, 후보별 무료배송 견적, 정책/상품 결과 조합, 실패 턴 미반영 |
| 14 | 실제 Spring AI vision 메시지, transcription, speech, image, moderation adapter. LIVE/파일 요구 |

native schema·pgvector·MCP 서버·멀티모달은 설정 및 기능을 지원하는 모델이 필요하다. 설정이 없을 때의 `UNSUPPORTED` 응답과 실제 실행 경로를 구분한다. MCP 서버 단원은 별도 애플리케이션 대신 동일 Tutor를 `--mcp-server`로 실행하여 프로세스 수를 줄였다.

## 테스트의 구체화

각 케이스마다 구조가 같은 클래스를 복제하는 대신 `CurriculumTest`의 dynamic test로 caseId별 실행과 입력 경계를 보고한다. 도구·금액·파싱·세션·취소·저장소·정책 필터의 실질적인 경계 테스트는 단원 패키지의 `*LessonTest`에 분리했다. MCP 3개 원격 케이스와 서버 노출은 별도 실제 서버 테스트에서 실행한다.

ProviderContractTest는 실제 OpenAI adapter에 로컬 HTTP 서버를 연결해 구조화 변환, tool call 왕복, SSE, vision 메시지 직렬화를 검증한다. MediaProviderContractTest는 이미지·전사·TTS·moderation의 실제 adapter 계약을 외부 비용 없이 검증한다.

LIVE 평가 태스크는 실제 텍스트·도구·임베딩/RAG·스트림·조건 회귀·무료배송 통합을 호출하고 보고서를 저장한다. 초기 설계의 30개 시퀀스×3회 품질 평가는 장기 확장 목표다. 현재 웹 회귀 수업은 2개 라벨 질문, LIVE smoke는 6개 테스트이며 전체 질문 분포에 대한 90% 품질 목표를 달성했다고 주장하지 않는다.

## 운영형 구현과 다른 학습 선택

- JDBC 수업은 실행별 격리 H2에서 repository 인스턴스를 교체해 복구한다. 서버 재시작 후 사용자 대화를 보존하는 서비스가 아니다.
- STUB 임베딩은 5차원 고정 단어군이고 LIVE는 설정한 실제 embedding model이다. 서로의 품질 점수를 비교하지 않는다.
- 웹 생성 정지는 스트리밍에서 제공한다. 일반 동기 REST 호출을 브라우저에서 끊었다고 서버 작업이 취소된다고 가정하지 않는다.
- 같은 대화는 실행 잠금으로 직렬화하며 실패 턴은 staging state를 commit하지 않는다. 세션별 최대 64개 대화, 기본 비활성 30분이다.
- 검색어 재작성은 Java에서 명시적으로 구성했다. 별도 고급 RAG 프레임워크 설정을 초기에 추가하지 않았다.
- 가격·배송 결과는 검증된 Java 데이터로 표시한다. 자유 형식 설명의 사실성 전체를 자동 보증하는 장치는 아니므로 L11에서 평가의 한계를 배운다.
- 기본/로컬 계약/웹 API 검증은 `test`, MCP는 `mcpIntegrationTest`, 실제 API는 `liveEvaluation`이다. 설계안의 `contractTest`·`webTest`는 별도 태스크로 만들지 않았다. 브라우저 수동 검증과 웹 API 자동 검증을 구분한다.

## 키와 실행 환경

실행과 테스트 스크립트는 모두 하위 프로젝트의 `scripts/`에 있다. 이 프로젝트 전용 `.env`가 없으면 기존 루트 `.env`를 재사용한다. 키를 Java 소스·YAML·정적 리소스에 넣지 않는다. 서버는 localhost에 바인딩하며 Actuator는 health/info만 노출한다. 학습 관찰 패널에는 키·전화번호를 마스킹한 전달 메시지를 제공한다.

이미지 모델 기본값과 이미지·전사 옵션은 [공식 이미지 API](https://developers.openai.com/api/reference/resources/images/methods/generate), [공식 전사 API](https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create)를 참고했다. 계정별 모델 권한이나 모델 운영 상태는 다를 수 있으므로 별도 환경변수로 변경할 수 있다.

## 검증 기록

2026-10-29 기준 검증 결과:

- 기본·로컬 provider 계약·웹 API: 158개 통과, 실패/skip 0개.
- 실제 MCP discovery·도구 호출·Tutor MCP 서버: 5개 통과.
- 실제 API를 사용하는 LIVE 평가: 6개 통과. 무료배송 목적을 상품명·카테고리로 잘못 추출하지 않는 회귀 검사 포함.
- 기존 모듈을 포함한 루트 `test bootJar` 통과.
- 셸 구문, 웹 JavaScript 구문, 문서 상대 링크 확인.
- pgvector 컨테이너와 실제 유료 이미지·음성 생성은 실행하지 않음. 미디어 adapter는 로컬 HTTP 계약 테스트로 확인.

LIVE 평가는 기본 설정의 `gpt-4.1-mini`와 `text-embedding-3-small`을 사용했다. 검증 질문은 가상 쇼핑 데이터이며 API 키를 결과 문서에 기록하지 않는다.

브라우저에서 LIVE 무료배송 추천의 현재 금액/가상 견적, 상품 카드와 실제 Java 소스 보기를 확인했다. [실행 화면](images/live-preview.png).

후속 가독성 개선에서 프런트엔드를 6개 역할별 JavaScript 모듈로 분리하고 HTML/CSS의 들여쓰기와 영역 설명을 정리했다. 단원 검색·일반 대화·소스 보기·초기화·스트리밍을 브라우저에서 확인했으며, 분할된 한글 바이트와 CRLF를 포함한 SSE 해석을 검증했다. [검증 화면](images/frontend-modules-preview.png).

테스트 공통 실행기를 `scripts/test/test.sh`로 옮기고 4개의 진입 스크립트를 추가했다. `test_live_mcp.sh`의 실제 모델 + 실제 MCP 통합 테스트 1개가 추가로 통과했다. 이 테스트는 기본 `test`나 기존 `liveEvaluation`에 자동 포함되지 않는 명시 실행 태스크다.
