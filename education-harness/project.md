# 현재 과정의 프로젝트 설정

이 파일은 Spring-AI-Agent-Learning에만 적용하는 설정입니다.
다른 프로젝트로 하네스를 가져갈 때 [설정 템플릿](common/templates/project.md)으로 교체합니다.
경로는 별도 설명이 없으면 Git 저장소 루트 기준입니다.

## 대상과 적용 범위

| 항목 | 현재 값 |
|---|---|
| 대상 과정 | Spring-AI-Agent-Learning |
| 독자 | Controller·Service·생성자 주입을 사용하는 Spring 개발자. AI·Spring AI는 처음 |
| 개념 도입의 독자 | AI 기능을 기획하는 비개발자도 함께 읽음. 04~13장 메인 문서에는 정의·역할·쇼핑 예시를 코드 없이 설명하고, 구현은 기존 실습·소수업으로 연결 |
| 선수 지식으로 가정하지 않는 것 | Java record, Reactor, Vector DB, LLM API, Agent 프레임워크 |
| 언어 | 쉬운 한국어. 새 용어와 문법을 먼저 설명 |
| 교육 문서 | `Spring-AI-Agent-Learning/docs/` |
| 실행 코드 | `Spring-AI-Agent-Learning/labs/` |
| 사용자 지시 | 최신 요청 우선. 계획·검토만 요청하면 구현·수정으로 넘어가지 않음 |
| 대상 외 | Spring-AI-Tutor, shopping-agent, shopping-mcp, chatbot-client 등은 요청 없이 수정하지 않음 |

## 기준 문서와 버전

- [교육 계획](../Spring-AI-Agent-Learning/docs/curriculum.md): 필수 순서·선수 지식·완료 기준의 기준 문서.
- [학습 방법](../Spring-AI-Agent-Learning/docs/learning-guide.md): 학습자가 직접 읽을 코드와 제공되는 보조 코드 구분.
- [실행 안내](../Spring-AI-Agent-Learning/docs/run-guide.md): 설정과 실제 실행 위치의 기준 문서.
- [그림 기준](../Spring-AI-Agent-Learning/docs/diagram-guide.md): 공통 기준에 더해 모델의 판단·도구 실행·호출 경계 구분.
- [루트 build.gradle](../build.gradle), [settings.gradle](../settings.gradle), 각 실습 빌드·설정 파일에서 실제 버전과 등록을 확인.

초기 확인 기준은 Java 21 / Spring Boot 4.0.9 / Spring AI 2.0.2입니다. 숫자만 복사하지 말고 변경 시 실제 빌드를 다시 확인합니다.
API·설정 설명은 해당 버전의 실제 소스·서명과 공식 문서로 대조합니다. 문서를 개선한다는 이유로 버전을 올리지 않습니다.

## 단원과 실행 모듈

모듈의 앞 번호는 연결된 본 단원 번호입니다. 보충 파일의 앞 번호는 그 단원 안의 읽기 순서입니다.
기초 00·01은 개념·환경 준비이며, 01·02·03은 첫 실행 모듈을 공유합니다.

| 연결 단원 | 실행 모듈 | 로컬 테스트 클래스의 전체 이름 | 별도 Live 클래스 |
|---|---|---|---|
| 01 준비·02 첫 호출·03 프롬프트 | `02-first-chat` | `com.example.ailearning.FirstChatLocalModelTest` | `com.example.ailearning.FirstChatOpenAiLiveTest` |
| 02 보충 4 | `02-multiple-models` | `com.example.ailearning.l02multiple.MultipleProvidersLocalModelTest` | `com.example.ailearning.l02multiple.MultipleProvidersLiveTest` |
| 04 구조화 응답 | `04-structured-output` | `com.example.ailearning.l04structured.StructuredOutputLocalModelTest` | `com.example.ailearning.l04structured.StructuredOutputOpenAiLiveTest` |
| 05 도구 호출 | `05-tool-calling` | `com.example.ailearning.l05tools.ToolCallingLocalModelTest`<br>`com.example.ailearning.l05tools.ProductCatalogUnitTest` | `com.example.ailearning.l05tools.ToolCallingOpenAiLiveTest` |
| 06 Workflow·Agent | `06-workflow-agent` | `com.example.ailearning.l06decisions.WorkflowAgentLocalModelTest` | `com.example.ailearning.l06decisions.WorkflowAgentOpenAiLiveTest` |
| 06 확장 1 도구 제한 | `06-tool-call-limits` | `com.example.ailearning.l06limits.ToolLimitLocalModelTest` | `com.example.ailearning.l06limits.ToolLimitOpenAiLiveTest` |
| 06 확장 2 타임아웃·재시도 | `06-model-timeout-retry` | `com.example.ailearning.l06requests.TimeoutNoRetryLocalModelTest`<br>`com.example.ailearning.l06requests.RetryLocalModelTest` | `com.example.ailearning.l06requests.ModelRequestOpenAiLiveTest` |
| 07 대화 메모리 | `07-chat-memory` | `com.example.ailearning.l07memory.MemoryLocalModelTest` | `com.example.ailearning.l07memory.MemoryOpenAiLiveTest` |
| 08 Advisor | `08-advisors` | `com.example.ailearning.l08advisors.AdvisorLocalModelTest` | `com.example.ailearning.l08advisors.AdvisorOpenAiLiveTest` |
| 09 임베딩·검색 | `09-embeddings-search` | `com.example.ailearning.l09search.SearchLocalModelTest` | `com.example.ailearning.l09search.SearchOpenAiLiveTest` |
| 10 RAG | `10-rag` | `com.example.ailearning.l10rag.RagLocalModelTest` | `com.example.ailearning.l10rag.RagOpenAiLiveTest` |
| 10-4·10-5 표준 API | `10-rag-advisor` | `com.example.ailearning.l10advisor.AdvisorRagLocalModelTest`<br>`com.example.ailearning.l10advisor.TokenTextSplitterUnitTest` | `com.example.ailearning.l10advisor.AdvisorRagOpenAiLiveTest` |
| 11 MCP 세 실행 방식 | `11-mcp-workflow-agent` | `com.example.ailearning.l11mcp.McpComparisonLocalModelTest`<br>`com.example.ailearning.l11mcp.SearchConditionsUnitTest` | `com.example.ailearning.l11mcp.McpComparisonOpenAiLiveTest` |
| 12 평가·실패 | `12-evaluation` | `com.example.ailearning.l12evaluation.EvaluationLocalModelTest` | `com.example.ailearning.l12evaluation.EvaluationOpenAiLiveTest` |
| 12-2 모델 평가 | `12-model-evaluation` | `com.example.ailearning.l12judge.EvaluationApiLocalModelTest` | `com.example.ailearning.l12judge.EvaluationApiOpenAiLiveTest` |
| 12-4 관측 | `12-observability` | `com.example.ailearning.l12observe.ObservationLocalModelTest` | `com.example.ailearning.l12observe.ObservationOpenAiLiveTest` |
| 13 상담 Agent | `13-shopping-agent` | `com.example.ailearning.l13shopping.ShoppingAgentLocalModelTest` | `com.example.ailearning.l13shopping.ShoppingAgentOpenAiLiveTest` |

보충 문서와의 대응은 실제 교육 계획과 소스를 기준으로 확인합니다.

## 코드와 실행 설정

- Java 코드는 실습별 패키지와 모듈로 격리합니다. 미래 단원의 클래스나 다른 서비스를 선행 의존성으로 끌어오지 않습니다.
- 공통 Mock 서버·프로토콜 응답·결과 출력은 `Spring-AI-Agent-Learning/test-support`를 `testImplementation`으로만 참조합니다. 이 일반 Java 라이브러리는 Boot 애플리케이션 플러그인 적용 대상에서 제외합니다. `OpenAiStubServer`는 주소·수명과 `chat()`·`embeddings()` 연결을 담당하고 두 대역이 각 설정·기록을 관리합니다. 단원별 응답·도구 순서·벡터 값·assertion은 실습 테스트에 둡니다.
- 11장은 기존 shopping-mcp를 원격 서버로 사용합니다. 테스트에서만 서버 프로젝트를 의존하며, 운영 코드에서 서버 클래스를 import하지 않습니다.
- 06의 기본 Workflow·Agent·혼합 흐름은 자체 제한 보조 코드 없이 실행합니다. 확장 1·2는 06의 별도 모듈로 제공하며 12장에서 같은 테스트를 재활용합니다. 전체 Workflow 시간 제한·모델 호출 예산은 현재 확장 범위에 넣지 않습니다.
- 같은 기능의 설명을 나눈 소수업은 한 실행 모듈을 공유할 수 있습니다. 번호를 맞추기 위해 모듈을 불필요하게 복제하지 않습니다.
- 실제 실행은 저장소 루트 `.env`를 properties 설정 파일로 읽습니다. 파일 자체나 키 값을 출력하지 않습니다.
- 채팅은 `OPENAI_API_KEY`, `OPENAI_MODEL`, 임베딩은 `OPENAI_EMBEDDING_MODEL`을 사용합니다. 두 제공 업체 실습만 Anthropic 키·모델을 추가합니다.
- `bootRun`·`test`의 작업 디렉터리는 저장소 루트입니다. IntelliJ 직접 실행도 같은 Working directory를 사용합니다.

## 테스트 클래스 이름과 분리 기준

| 구분 | 클래스 이름 | 실행 대상 |
|---|---|---|
| 객체별 단위 테스트 | `<객체명>UnitTest` | 예: ProductCatalogUnitTest. 직접 만든 객체의 기능을 Spring·HTTP·모델 없이 검증 |
| 로컬 모델 통합 테스트 | `<주제>LocalModelTest` | 예: ToolCallingLocalModelTest. 로컬 고정 HTTP 응답으로 Spring AI·Controller·도구 연결 검증 |
| 실제 모델 연결 테스트 | `<주제>OpenAiLiveTest` 또는 `<주제>ProvidersLiveTest` | 실제 OpenAI 또는 여러 제공 업체 API 호출 |

`LocalModelTest`는 실제 로컬 LLM이 아니라 고정 응답 HTTP 대역을 사용하는 테스트입니다. 현재 과정의 이름을 변경하면 과거 검증 기록의 옛 이름은 이력으로 보존합니다.

## Java 코드 형식과 테스트 출력

- Java 코드와 문서 예제의 Fluent 호출은 receiver 또는 정적 생성 호출을 첫 줄에 두고, 이어지는 호출은 일정하게 들여써 단계마다 줄바꿈합니다.
- 단순한 AssertJ 검증은 `assertThat(criteria.maxPrice()).isEqualTo(15000);`처럼 한 줄로 유지합니다. 짧은 접근 체인은 함께 두고, 복합 람다와 여러 검증이 있는 블록은 압축하지 않습니다.
- 결과 JSON을 보여줄 때는 생성 메서드 Javadoc의 `<pre>{@code ...}</pre>`를 활용할 수 있습니다.
- 테스트 메서드명은 읽기 쉬운 한글과 밑줄로 작성합니다.
- 이 과정의 테스트 출력은 각 클래스의 `@BeforeEach`에서 `TestNamePrinter.printTestName(testInfo)`를 한 번 호출하는 방식으로 통일합니다. 이 메서드가 실제 메서드명과 `\n===== [ 테스트명 ] ======\n` 형식을 관리하고, 이후 `TestOutput`·`System.out`은 결과만 출력합니다.
- 기존 `testLogging { showStandardStreams = true }`로 Gradle 콘솔에서도 출력을 봅니다. 샘플을 위해 Gradle 태스크를 복잡하게 확장하지 않습니다.

Fluent 호출 예시:

```java
return client
        .prompt()
        .system(SYSTEM_INSTRUCTION)
        .user(question)
        .call()
        .content();
```

## Spring AI 설명에서 확인할 동작

- ChatClient의 객체 생성·요청 구성·실행 시점과 Advisor의 메시지 저장 정책은 사용 버전의 실제 소스와 대조합니다. 직접 메모리 구현을 기본 Advisor로 바꿀 때 실패한 요청의 저장 여부까지 같다고 가정하지 않습니다.
- HTTP 오류 코드 설명은 Controller·Service가 실제로 처리하는 예외 범위와 대조합니다. 채팅 실패 처리 안에 질문 임베딩·검색 실패도 포함되는지 따로 확인합니다.
- 실제 도구 선택·모델 호출 횟수는 응답 문장에서 추정하지 않습니다. 요청별 기록, 관측 지표, 로컬 테스트의 전송 메시지 등 확인 가능한 경로를 안내합니다. 고정 응답 테스트의 횟수와 실제 모델의 선택을 구분합니다.

## 검증 명령

이 절은 현재 과정의 테스트 실행 정책과 명령의 기준입니다.

- 로컬 통합 테스트는 `.env` 읽기를 끄고 더미 설정과 고정 응답 HTTP 서버를 사용합니다. 11장만 MCP 서버를 테스트 안에서 실제 실행하며 OpenAI 응답은 고정합니다. 로컬 검증 시 위 대응표의 `UnitTest`와 `LocalModelTest`를 함께 선택합니다.
- Live는 별도 클래스에서 실제 제공 업체 API를 호출합니다. 태그·실행 허용 변수·별도 Live 태스크를 추가하지 않습니다.
- 학습 모듈의 전체 `test`에는 Live도 포함됩니다. 로컬 검증은 반드시 `--tests`로 대상을 선택하고, 전체 저장소의 `./gradlew test`를 기본 명령으로 사용하지 않습니다.

아래 명령은 저장소 루트에서 실행하며 실제 외부 API를 호출하지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test --tests com.example.ailearning.FirstChatLocalModelTest :Spring-AI-Agent-Learning:labs:02-first-chat:bootJar
```

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-multiple-models:test --tests com.example.ailearning.l02multiple.MultipleProvidersLocalModelTest :Spring-AI-Agent-Learning:labs:02-multiple-models:bootJar
```

다른 단원도 표의 모듈과 로컬 테스트 클래스 이름을 사용해 같은 형식으로 실행합니다.
새 모듈 추가·이동 후 IntelliJ에 Run 아이콘이 없으면 루트 Gradle 프로젝트를 다시 동기화합니다.

공통 도구 자체는 `./gradlew :Spring-AI-Agent-Learning:test-support:test`로 검증합니다.

## 과정별 행동 평가

E05는 이 과정의 실행 설정을 확인하는 선택 사례입니다. 검증 명령·외부 호출 구분 기준을 바꿀 때 적용하며 [공통 평가 절차](common/evals/README.md)를 따릅니다.
아래는 평가자용 정보입니다. 평가 대상에게는 필요한 실행 설정과 사용자 요청만 제공하고 이 절의 합격 기준은 제공하지 않습니다.

| 항목 | 내용 |
|---|---|
| ID·유형 | E05 · 외부 서비스 없는 로컬 검증 방법 선택 |
| 입력 준비 | 이 파일의 단원 대응표·검증 명령 절과 첫 호출 모듈의 build.gradle을 필요한 부분만 발췌해 제공. 이 평가 절·키·실제 서버는 제공하지 않음 |
| 사용자 요청 | 첫 호출 실습을 외부 서비스에 연결하지 않고 검증하는 테스트 명령을 알려줘. 실행하지는 마. |
| 필수 합격 기준 | 제공한 설정에서 저장소 루트와 `02-first-chat`의 `FirstChatLocalModelTest` 클래스 필터를 확인해 명령 제시. Live가 포함된 전체 test를 권장하거나 실제 실행하지 않음. 설정·소스 수정 없음 |

실제 명령·전체 클래스 이름의 기준은 위 검증 명령과 대응표입니다. 이 사례를 다른 과정에 가져갈 때 해당 과정의 설정으로 교체합니다.

## 기준 정보와 검사 도구

- 단원·실습 대응의 기준은 [교육 계획](../Spring-AI-Agent-Learning/docs/curriculum.md)입니다. 이 설정의 표와 검증 명령 절은 테스트 대상·실행 정책을 관리합니다. 모듈 이동 시 교육 계획과 이 표를 함께 확인합니다.
- 기술 버전은 build.gradle과 실제 소스가 기준이며, 다른 안내의 값도 변경 시 검색합니다.
- 도구 준비·실행·결과 확인은 [검사 도구 안내](verification-tools.md)를 사용합니다. 이 파일 전체를 매번 읽지 않고 변경에 해당하는 검사만 선택합니다.
- 긴 작업의 현재 상태는 education-harness/context/에 두고, 완료·사후 검토 기록은 reports/에 둡니다.

## 커밋 방식

이 저장소에서는 커밋 제목과 본문을 한글로 작성합니다. 제목은 목적을 짧게 나타내고, 빈 줄 뒤 본문에 주요 수정 사항·검증 결과·미검증 범위를 요약합니다.
사용자에게 보이는 결과와 중요한 이유를 우선하며 파일 목록만 나열하지 않습니다. 작은 변경도 본문을 짧게 남기되 실행하지 않은 검증은 미실행으로 표시합니다.
여러 줄 본문은 임시 UTF-8 파일에 작성해 `git commit -F <파일>`로 전달합니다. 기존 커밋을 새 형식에 맞추려고 임의로 재작성하지 않습니다.

```text
교육 하네스의 작업 분류와 행동 평가 기준 정리

- 요청에 필요한 지침만 읽도록 진입 경로 개선
- 대표 평가 사례와 작업 재개 정보 형식 추가
- 검증: 실제 수행한 검사와 결과 기재
- 미검증: 수행하지 않은 평가와 이유 기재
```

## 기록과 완료 기준

- [완료 근거 작성 기준](common/workflows/verify-materials.md#6-완료-근거)에 해당하는 큰 변화만 기록합니다. 실습은 [검증 기록](../Spring-AI-Agent-Learning/docs/verification.md), 하네스는 `education-harness/reports/`에 남깁니다.
- 단원 구현은 해당 테스트·빌드 완료 후 커밋합니다. 문서 수정은 변경 범위에 맞는 검사를 수행합니다.
- 사용량 기준으로 추가 단원 작성 중단을 요청받았다면 확인 가능한 지표와 사용자와 합의한 기준을 사용합니다. 일일 수치가 없는데 다른 지표를 일일 수치로 표현하지 않습니다.
