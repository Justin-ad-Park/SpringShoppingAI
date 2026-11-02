# 교육 실습 검증 기록

최초 작성: 2026-10-29. 저장소 내부로 이동 후 재검증: 2026-11-02.
Java 21 / Gradle 8.14.5 / Boot 4.0.9 / Spring AI 2.0.2를 저장소 전체에서 공통 사용합니다.
전체 마이그레이션과 232개 테스트 결과는 [변경 기록](../../docs/SPRING-AI-2-MIGRATION.md)을 참고하세요.

## 확인한 범위

```bash
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:test :Spring-AI-Agent-Learning:labs:01-first-chat:bootJar --no-daemon
```

- Java 코드와 테스트 코드 컴파일 성공.
- 실제 Spring Boot 서버 + 실제 Spring AI 어댑터 + 로컬 가짜 모델 API를 연결한 테스트 3개 통과.
- 사용자 질문·모델 이름·더미 인증 헤더가 외부 API 형식으로 전달되고, 고정 응답이 HTTP 응답으로 돌아옴을 확인.
- 공백·2000자 초과 입력이 HTTP 400으로 거부되고 모델 호출이 발생하지 않음을 확인.
- 두 번째 호출에 첫 번째 질문이 포함되지 않음을 확인. 자동 대화 메모리가 없는 상태를 검증.
- 실행 가능한 Boot JAR 빌드 성공.
- Markdown 로컬 파일 링크의 대상 존재 확인.
- 01장의 Gradle 의존성·전체 YAML 예제, 02장의 전체 Java 클래스가 실제 소스와 일치함을 확인.

테스트 보고서는 저장소 루트 기준 `Spring-AI-Agent-Learning/labs/01-first-chat/build/reports/tests/test/index.html`입니다.
JAR은 `Spring-AI-Agent-Learning/labs/01-first-chat/build/libs/01-first-chat-0.1.0.jar`에 생성됩니다.

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
| 합계 | 독립 모듈 4개 | 26 | 실패·스킵 없음 |

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
| 합계 | 독립 모듈 5개 | 28 | 실패·스킵 없음 |

각 모듈의 test와 bootJar를 실행한 뒤 단원별로 커밋했습니다.
마지막 회귀 검증 명령은 다음과 같습니다.

```bash
./gradlew test bootJar :Spring-AI-Tutor:mcpIntegrationTest :integration-tests:smokeTest :integration-tests:gptPathTest --console=plain
```

결과: BUILD SUCCESSFUL. 기본 test 260개 + MCP 통합 6개 + smoke 17개 + GPT 경로 3개 = **286개 통과**, 실패·오류·스킵 없음.
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
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:test --tests com.example.ailearning.FirstChatLocalMockModelTest
# 두 클래스 모두 실행
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:test
```

두 클래스 전체 실행으로 4개 통과, 실패·스킵 없음과 실제 응답 출력을 확인했습니다.
실제 키는 소스·YAML에 기록하지 않습니다. 질문과 응답은 표준 출력과 test 보고서에서 확인합니다.
이는 단일 요청의 연결·응답 확인이며 전체 단원의 실제 모델 품질을 검증한 결과는 아닙니다.
현재 보고서: `labs/01-first-chat/build/reports/tests/test/index.html`.
