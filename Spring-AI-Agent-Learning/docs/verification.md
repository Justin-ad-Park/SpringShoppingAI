# 교육 실습 검증 기록

최초 작성: 2026-10-29. 저장소 내부로 이동 후 재검증: 2026-11-02.
Java 21 / Gradle 8.14.5 / Boot 4.0.9 / Spring AI 2.0.2를 저장소 전체에서 공통 사용합니다.
전체 마이그레이션과 232개 테스트 결과는 [변경 기록](../../docs/SPRING-AI-2-MIGRATION.md)을 참고하세요.

## 확인한 범위

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test :Spring-AI-Agent-Learning:labs:02-first-chat:bootJar --no-daemon
```

- Java 코드와 테스트 코드 컴파일 성공.
- 실제 Spring Boot 서버 + 실제 Spring AI 어댑터 + 로컬 가짜 모델 API를 연결한 테스트 3개 통과.
- 사용자 질문·모델 이름·더미 인증 헤더가 외부 API 형식으로 전달되고, 고정 응답이 HTTP 응답으로 돌아옴을 확인.
- 공백·2000자 초과 입력이 HTTP 400으로 거부되고 모델 호출이 발생하지 않음을 확인.
- 두 번째 호출에 첫 번째 질문이 포함되지 않음을 확인. 자동 대화 메모리가 없는 상태를 검증.
- 실행 가능한 Boot JAR 빌드 성공.
- Markdown 로컬 파일 링크의 대상 존재 확인.
- 01장의 Gradle 의존성·전체 YAML 예제, 02장의 전체 Java 클래스가 실제 소스와 일치함을 확인.

테스트 보고서는 저장소 루트 기준 `Spring-AI-Agent-Learning/labs/02-first-chat/build/reports/tests/test/index.html`입니다.
JAR은 `Spring-AI-Agent-Learning/labs/02-first-chat/build/libs/02-first-chat-0.1.0.jar`에 생성됩니다.

## 확인하지 않은 범위

최초 검증에서는 실제 API 키와 유료 모델 호출을 실행하지 않았습니다. 첫 실습의 후속 실제 호출 결과는 아래 추가 기록을 참고하세요.
실제 모델의 접근 권한·자연어 품질·비용·응답 시간은 이번 로컬 테스트가 보장하지 않습니다.
교재의 자연어 응답은 형태 예시이며 실제 실행 결과로 표시하지 않았습니다.
03장의 프롬프트 수정·실제 답변 비교는 학습자가 수행하는 실험입니다.
MCP 교육 실습과 선택 심화는 아직 구현·검증 완료로 간주하지 않습니다.

## 재검증할 때

버전이나 코드를 바꾸면 위 명령을 다시 실행합니다.
system 메시지를 추가하는 03장 실험 중에는 원본 메시지 수를 검증하는 테스트가 실패할 수 있으므로,
실험을 마치고 원본으로 복원한 후 테스트를 실행합니다.

## 04~07 추가 검증 — 2026-11-02

| 단원 | 실행 모듈 | 테스트 | 결과 |
|---|---|---:|---|
| 04 구조화 응답 | 04-structured-output | 5 | 통과 |
| 05 Tool Calling | 05-tool-calling | 6 | 통과 |
| 06 Workflow/Agent 비교 | 06-workflow-agent | 8 | 통과 |
| 07 대화 메모리 | 07-chat-memory | 7 | 통과 |
| 합계 | 독립 모듈 4개 | 26 | 전체 테스트 통과(실패 0개, 건너뜀 0개) |

각 모듈의 test와 bootJar를 단원별로 실행한 후 커밋했습니다.
추가로 저장소 루트에서 `./gradlew test bootJar --console=plain` 회귀 빌드가 성공했습니다.
해당 명령의 기존 변경 없는 작업은 Gradle 캐시를 사용합니다. 기존 MCP/쇼핑 통합 검증 기록과는 별도입니다.

주요 검증:

- 04: 실제 모델 어댑터를 통한 객체 변환, null 유지, 형식 실패/업무 값 실패, 빈 질문 검증.
- 05: 직접 Java 호출은 모델 요청 0회, 모델 도구 요청 → Java 실행 → 실제 결과 재전달, 도구 없는 답변과 가격 검증.
- 06: 동일 요청의 고정 단계/모델 선택/Java 후검증 비교, 실제 모델 요청 횟수, 도구 결과 기록, 예산 누락·결과 없음, Java 호출 상한과 요청 대기 시간 상한.
- 07: 이전 user/assistant 메시지가 다음 모델 API 요청에 포함됨, 쿠키 세션별·대화 ID별 격리, 삭제 범위, 6개 저장 창과 최대 8개 전송, 빈 응답 미저장.
- 교재의 9개 Mermaid 블록은 임시 Node 환경의 Mermaid 파서로 문법 검증했습니다. 픽셀 단위 렌더링 검증을 했다는 뜻은 아닙니다.
- 문서의 로컬 링크와 핵심 Java 코드 블록을 확인했습니다. 각 실습은 자기 패키지만 import하며 다른 실습/프로젝트 의존성이 없습니다.

모든 신규 테스트는 로컬 고정 모델 서버를 사용합니다. 실제 모델의 언어 이해·도구 선택 정확도와 비용은 평가하지 않았습니다.
신규 실습의 실제 유료 모델 호출도 실행하지 않았습니다.

## 08~12 추가 검증 — 2026-11-02

| 단원 | 실행 모듈 | 테스트 | 결과 |
|---|---|---:|---|
| 08 Advisor | 08-advisors | 4 | 통과 |
| 09 임베딩과 검색 | 09-embeddings-search | 4 | 통과 |
| 10 RAG | 10-rag | 6 | 통과 |
| 11 평가와 운영 기초 | 11-evaluation | 5 | 통과 |
| 12 통합 상담 Agent | 12-shopping-agent | 9 | 통과 |
| 합계 | 독립 모듈 5개 | 28 | 전체 테스트 통과(실패 0개, 건너뜀 0개) |

각 모듈의 test와 bootJar를 실행한 뒤 단원별로 커밋했습니다.
마지막 회귀 검증 명령은 다음과 같습니다.

```bash
./gradlew test bootJar :Spring-AI-Tutor:mcpIntegrationTest :integration-tests:smokeTest :integration-tests:gptPathTest --console=plain
```

결과: BUILD SUCCESSFUL. 기본 test 260개 + MCP 통합 6개 + smoke 17개 + GPT 경로 3개 = **286개 통과**, 실패 0개, 오류 0개, 건너뜀 0개.
변경 없는 작업은 Gradle의 up-to-date 결과를 재사용했습니다. 전체 기본 test에는 교육 실습 57개가 포함됩니다.
이 명령에 포함하지 않은 live 태스크의 과거 XML 결과는 합산하지 않았습니다.

주요 검증:

- 08: Advisor의 진입/반환 순서, 도구 루프 밖 1회와 안쪽 반복, 메모리 중복 저장 방지와 세션 격리.
- 09: 실제 Embedding 어댑터의 요청과 SimpleVectorStore 적재·검색·임계값·필터, 문자열 검색과 벡터 검색 비교.
- 10: 문서 분할·검색 근거 전달, 근거 없을 때 생성 생략, 허위/누락 출처 거부, 모델 보류·형식 실패.
- 11: 고정 사례 평가, 출처가 맞아도 내용이 틀린 경우 실패, 실제 응답 usage와 지연 계측, 429 무재시도와 SDK 시간 제한.
- 12: 상품·배송비·정책 결합, 후속 조건 유지, 배송비로 예산 초과, 의도별 허용 도구, 허위 ID/예산 초과 거부,
  실패 시 기존 대화 유지, 세션 격리·삭제, 도구 상한과 시간 초과 후 늦은 응답 미저장.
- 11 검증 중 자동 설정의 SDK 전송 옵션 위치를 바로잡았습니다. `chat.max-retries`, `chat.timeout`을 사용하며
  `chat.options` 아래에 두지 않습니다. 해당 실습과 기존 shopping-agent 설정도 회귀 검증했습니다.
- 현재 교재의 Mermaid 19개를 파서로 검증했습니다. 그림 렌더링의 픽셀 검증은 수행하지 않았습니다.
- Markdown 로컬 링크 151개 대상 존재와 08~12 핵심 Java 코드 블록의 소스 일치, 모듈 간 의존성 부재를 확인했습니다.

테스트는 실제 Spring AI 어댑터와 로컬 고정 응답 HTTP 서버를 연결합니다.
유료 모델 호출은 실행하지 않았으며, 실제 언어 이해·임베딩 품질·도구 선택 정확도는 학습자의 별도 평가 대상입니다.
실제 실행 시의 모델 접근 권한·API 비용·운영 동시성·인증 완성도를 보장하는 검증은 아닙니다.

## 첫 실습의 현재 테스트 구성 — 2026-11-02

두 테스트를 연결 대상에 따라 별도 클래스로 두었습니다. 태그·실행 허용 변수·별도 Live 태스크·클래스 감지 로직은 없습니다.

| 클래스 | 연결 대상 | 검증 결과 |
|---|---|---|
| `FirstChatLocalMockModelTest` | 로컬 MockServer와 더미 키 | 3개 통과 |
| `FirstChatOpenAiLiveTest` | 실제 OpenAI, 루트 `.env`의 `OPENAI_API_KEY`·`OPENAI_MODEL` | 1개 통과·응답 출력 확인 |

IntelliJ에서는 각 클래스 또는 메서드의 Run을 사용합니다.
Gradle의 필터 없는 `test`도 두 클래스를 모두 실행하며 실제 API 사용량이 발생합니다.
`build.gradle`에는 작업 디렉터리와 테스트 출력 설정만 추가합니다.

```bash
# Mock만 실행 (실제 키 불필요)
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test --tests com.example.ailearning.FirstChatLocalMockModelTest
# 두 클래스 모두 실행
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test
```

두 클래스 전체 실행으로 테스트 4개가 모두 통과했고(실패 0개, 건너뜀 0개), 실제 응답 출력도 확인했습니다.
실제 키는 소스·YAML에 기록하지 않습니다. 질문과 응답은 표준 출력과 test 보고서에서 확인합니다.
이는 단일 요청의 연결·응답 확인이며 전체 단원의 실제 모델 품질을 검증한 결과는 아닙니다.
현재 보고서: `labs/02-first-chat/build/reports/tests/test/index.html`.

## 초급 과정 개편과 표준 API 실행 실습 — 2026-11-03

Spring Academy의 개념 → 구현 → 실습 구분을 참고해 기존 04~12 설명을 29개 소수업으로 분리했습니다.
표준 API를 보완하는 설명 4개를 더해 소수업은 33개이며, 추가 구현은 3개의 독립 실행 모듈입니다.
기존 전체 코드를 한 번에 늘리는 대신 기능별 애플리케이션을 분리하고, 소수업마다 실행 모듈·설정·테스트 링크를 제공합니다.
00~03에는 읽기 중단 지점과 관찰 기준을 추가했습니다.

| 추가 실행 모듈 | 테스트 | 확인한 실제 Spring AI 연결 |
|---|---:|---|
| 10-rag-advisor | 4 | TextReader·TokenTextSplitter·저장소·QuestionAnswerAdvisor, 문서 프롬프트 전달, 빈 검색의 생성 호출 |
| 11-model-evaluation | 4 | RelevancyEvaluator·FactCheckingEvaluator의 평가 프롬프트와 YES/NO 해석 |
| 11-observability | 2 | Micrometer·Actuator의 실제 모델 타이머·token.type:total 토큰 Counter 조회 |
| 합계 | 10 | 실패 0개, 오류 0개, 건너뜀 0개 |

각 모듈의 test와 bootJar를 통과한 뒤 커밋했습니다. 시작 클래스·Controller·Service·설정·테스트를 모두 포함합니다.
각 모듈의 main Java는 3개 파일이며 각각 54·60·33줄입니다. 별도 실행 프레임워크나 계측 보조 클래스를 런타임에 만들지 않았습니다.
테스트의 로컬 HTTP 모델 서버는 제공된 검증 도구로 분리했습니다.

최종 회귀 검증:

```bash
./gradlew test bootJar :Spring-AI-Tutor:mcpIntegrationTest :integration-tests:smokeTest :integration-tests:gptPathTest -x :Spring-AI-Agent-Learning:labs:02-first-chat:test --console=plain
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test --tests com.example.ailearning.FirstChatLocalMockModelTest --console=plain
```

두 명령 모두 BUILD SUCCESSFUL. 기본 테스트 보고서 270개 + MCP 통합 6개 + smoke 17개 + GPT 경로 3개 = **296개 통과**.
변경 없는 작업은 Gradle up-to-date 결과를 재사용했습니다. 과거 live 태스크의 결과는 합산하지 않았습니다.
첫 모듈의 실제 OpenAI 테스트는 이번 검증에서 실행하지 않았고 Mock 클래스만 실행했습니다.
첫 모듈의 단순한 두 테스트 클래스와 전체 test 실행 시 두 클래스가 실행되는 동작은 변경하지 않았습니다.

문서 검증: 로컬 링크 388개 대상 존재, Mermaid 23개 파서 통과, 전체 Java 클래스 블록 18개가 실제 소스와 일치.
Mermaid의 문법을 검사했으며 픽셀 단위 그림 검증을 수행했다는 의미는 아닙니다.

신규 모듈은 실제 모델로 실행 가능한 bootRun 설정과 요청 예제를 제공합니다.
이번 자동 검증은 실제 Boot·Spring AI 어댑터와 로컬 고정 HTTP 응답을 연결했으며, 새 모듈의 유료 외부 모델 호출은 수행하지 않았습니다.
실제 언어 이해·검색 품질·평가 모델 판정의 정확도는 각 수업의 실제 모델 관찰 실험에서 따로 확인합니다.

특히 토큰 지표의 input·output·total을 필터 없이 더하면 중복 합산됩니다.
실제 Actuator 응답을 확인한 뒤 `gen_ai.token.type:total` 필터를 적용해 입력 10+출력 5=15 증가를 검증했습니다.
평가와 관측의 구현·설명을 나누고 이러한 측정 단위 차이도 교재에 반영했습니다.

## 02 선택 실습: 여러 제공 업체 — 2026-11-03

- `02-multiple-models:test`의 테스트 2개 통과, `bootJar` 성공.
- 실제 OpenAI/Anthropic 어댑터와 로컬 HTTP 서버로 모델 이름·인증 정보·호출 대상의 분리를 검증했습니다.
- 기본 Builder 자동 설정을 끈 상태에서 두 ChatModel이 자동 설정되고, 이름으로 지정한 ChatClient가 주입됨을 확인했습니다.
- 실제 유료 모델 호출은 실행하지 않았습니다.

### 같은 제공 업체의 요청별 모델 선택 추가

- `SameProviderModelsService`를 추가하고 전체 테스트 3개와 `bootJar`가 통과했습니다.
- 요청 옵션으로 OpenAI 모델 A와 B를 선택한 뒤 기본 모델 호출이 원래 설정을 유지하는지 검증했습니다.
- 세 요청이 같은 OpenAI 인증 정보를 사용하고 Anthropic 호출이 발생하지 않음을 확인했습니다. 실제 유료 API는 호출하지 않았습니다.

## 입문 설명 정리와 루트 .env 통일 — 2026-11-03

- 14개 학습 모듈 모두 실제 실행 시 루트 `.env`를 읽고, 채팅 모델 설정은 `OPENAI_MODEL`로 통일했습니다. 임베딩 사용 모듈만 `OPENAI_EMBEDDING_MODEL`을 추가로 사용합니다.
- 각 모듈의 `bootRun`·`test` 작업 디렉터리는 저장소 루트입니다. 로컬 가짜 모델 테스트는 `spring.config.import=`로 파일 읽기를 끄고 기존 더미 키·모델·로컬 주소를 사용합니다.
- 14개 모듈의 테스트 70개가 모두 통과했고(실패 0개, 건너뜀 0개), 각 `bootJar` 빌드가 성공했습니다. 변경 없는 작업은 Gradle의 up-to-date 결과를 재사용했습니다.
- 첫 모듈은 `--tests com.example.ailearning.FirstChatLocalMockModelTest`로 로컬 테스트만 실행했습니다. 실제 OpenAI Live 테스트와 유료 외부 API는 실행하지 않았습니다.
- 앞서 승인 검토 오류로 보류됐던 입문 실습 검증도 이번 실행에서 완료했습니다.
- 검토 대상 입문 문서 8개의 기존 Java 예제를 보존했음을 비교했고, Markdown 로컬 링크 422개와 Mermaid 26개의 문법 검사를 통과했습니다. 다이어그램의 픽셀 단위 렌더링 검증은 수행하지 않았습니다.

## 02장 실습 번호 정리와 IntelliJ 테스트 인식 — 2026-11-03

- 첫 호출 실습을 `labs/02-first-chat`으로 옮기고 Gradle 등록·명령·문서 링크를 수정했습니다. 다중 모델 보충 실습은 `labs/02-multiple-models`를 유지합니다.
- 변경 전 다중 모델 클래스를 명시한 Gradle 실행으로 테스트 3개가 모두 통과했습니다.
- IntelliJ에서 다중 모델 모듈이 Gradle에 동기화되지 않아 테스트 소스가 상위 labs 디렉터리 소속으로 보이고 Run 아이콘이 없는 상태를 확인했습니다. 동기화 후 `02-multiple-models.test` 소속과 Run 항목이 나타났습니다.
- IDE와 터미널의 동시 테스트 실행에서 XML 결과 파일 쓰기 충돌이 한 차례 발생했습니다. 테스트 자체 실패와 구분해야 하며 같은 모듈은 한쪽에서만 실행합니다.
- 변경된 `02-first-chat`의 로컬 Mock 테스트와 `02-multiple-models` 테스트·두 모듈의 `bootJar` 빌드가 성공했습니다. 검증 명령은 첫 모듈의 실제 OpenAI 테스트를 제외했습니다.

## 다중 모델 Live 테스트 추가 — 2026-11-03

- `MultipleProvidersLiveTest`를 별도 클래스로 추가했습니다. 루트 `.env`의 두 업체 키·모델 설정을 읽고 서비스에서 실제 OpenAI와 Anthropic을 각각 호출하도록 구성했습니다.
- 업체별 질문·응답을 출력하며, 응답이 비어 있지 않은지 확인하는 테스트 2개입니다. 태그·실행 조건·별도 Live 태스크는 없습니다.
- 두 Live 메서드까지 포함한 테스트 소스 컴파일과 `bootJar` 빌드가 성공했습니다. `--tests com.example.ailearning.l02multiple.MultipleProvidersContractTest`로 기존 로컬 테스트 3개가 모두 통과했습니다(실패 0개, 건너뜀 0개).
- 실제 Live 메서드와 유료 외부 API는 이번 검증에서 실행하지 않았습니다. API 키·접근 권한·실제 응답 결과는 검증하지 않았습니다.
- 실행 가이드와 교재에 전체 test 실행 시 Live 테스트도 실행되는 점과 로컬 클래스 선택 명령을 반영했습니다. Markdown 로컬 링크 424개 검사 통과.

## 03~12장 설명과 실습 흐름 정리 — 2026-11-04

- 검토에서 찾은 19개 개선 항목을 반영했습니다. 생성자·필드·호출 관계, 호출 실행 시점, 로컬 테스트 선택, 수정 전후 비교·재시작, 호출 기록의 확인 경로, 메모리 저장 시점, RAG 오류 처리 범위, 사용량과 시간 측정 코드, 종합 검증의 한계를 정리했습니다.
- 단원별 확인 질문으로 반복 문장을 대체하고, 제작 이력을 학습 안내로 바꿨습니다.
- 08장에 `failedModelCallLeavesUserMessageInMemory`를 추가했습니다. 로컬 모델 API가 503으로 실패한 뒤 다음 요청에 이전 user 메시지가 전달되고, 실패한 호출의 assistant 메시지는 저장되지 않는 것을 검증했습니다. 애플리케이션의 기능 구현은 변경하지 않았습니다.
- 검증 명령: `./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test --tests com.example.ailearning.FirstChatLocalMockModelTest :Spring-AI-Agent-Learning:labs:06-workflow-agent:test :Spring-AI-Agent-Learning:labs:08-advisors:test :Spring-AI-Agent-Learning:labs:10-rag:test :Spring-AI-Agent-Learning:labs:11-evaluation:test :Spring-AI-Agent-Learning:labs:12-shopping-agent:test :Spring-AI-Agent-Learning:labs:08-advisors:bootJar`.
- 결과: 관련 6개 모듈의 테스트 결과 총 36개 통과 / 실패 0개 / 오류 0개 / 건너뜀 0개. 08장 5개는 이번에 실행했고, 나머지 31개는 Gradle의 up-to-date 통과 결과를 재사용했습니다. 08장 bootJar도 up-to-date 성공 결과를 재사용했습니다.
- 문서 검사: docs 아래 로컬 파일·제목 앵커 링크 403개와 03~12장의 전체 Java 코드 블록 15개를 대조했습니다. Mermaid 23개는 jsdom·Mermaid 파서로 문법 검사했으며, 실제 그림 렌더링은 확인하지 않았습니다. `git diff --check` 통과.
- 검토 방식: 단일 에이전트가 초급 학습 순서와 기술 동작을 순차 대조했습니다. 기본 메모리 Advisor의 저장 시점과 ChatClient의 호출 시점은 설치된 Spring AI 2.0.2 소스로 확인했습니다.
- 실제 외부 모델 호출과 Live 테스트는 실행하지 않았습니다. 실제 모델의 답변·검색·도구 선택 품질은 이번 검증 범위가 아닙니다.
