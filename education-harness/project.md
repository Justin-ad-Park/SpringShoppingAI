# 현재 과정의 프로젝트 설정

이 파일은 Spring-AI-Agent-Learning에만 적용하는 설정입니다.
다른 프로젝트로 하네스를 가져갈 때 [설정 템플릿](common/templates/project.md)으로 교체합니다.
경로는 별도 설명이 없으면 Git 저장소 루트 기준입니다.

## 대상과 적용 범위

| 항목 | 현재 값 |
|---|---|
| 대상 과정 | Spring-AI-Agent-Learning |
| 독자 | Controller·Service·생성자 주입을 사용하는 Spring 개발자. AI·Spring AI는 처음 |
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
| 01 준비·02 첫 호출·03 프롬프트 | `02-first-chat` | `com.example.ailearning.FirstChatLocalMockModelTest` | `com.example.ailearning.FirstChatOpenAiLiveTest` |
| 02 보충 4 | `02-multiple-models` | `com.example.ailearning.l02multiple.MultipleProvidersContractTest` | `com.example.ailearning.l02multiple.MultipleProvidersLiveTest` |
| 04 구조화 응답 | `04-structured-output` | `com.example.ailearning.l04structured.StructuredOutputContractTest` | `com.example.ailearning.l04structured.StructuredOutputOpenAiLiveTest` |
| 05 도구 호출 | `05-tool-calling` | `com.example.ailearning.l05tools.ToolCallingContractTest` | `com.example.ailearning.l05tools.ToolCallingOpenAiLiveTest` |
| 06 Workflow·Agent | `06-workflow-agent` | `com.example.ailearning.l06decisions.WorkflowAgentContractTest` | `com.example.ailearning.l06decisions.WorkflowAgentOpenAiLiveTest` |
| 07 대화 메모리 | `07-chat-memory` | `com.example.ailearning.l07memory.MemoryContractTest` | `com.example.ailearning.l07memory.MemoryOpenAiLiveTest` |
| 08 Advisor | `08-advisors` | `com.example.ailearning.l08advisors.AdvisorContractTest` | `com.example.ailearning.l08advisors.AdvisorOpenAiLiveTest` |
| 09 임베딩·검색 | `09-embeddings-search` | `com.example.ailearning.l09search.SearchContractTest` | `com.example.ailearning.l09search.SearchOpenAiLiveTest` |
| 10 RAG | `10-rag` | `com.example.ailearning.l10rag.RagContractTest` | `com.example.ailearning.l10rag.RagOpenAiLiveTest` |
| 10-4·10-5 표준 API | `10-rag-advisor` | `com.example.ailearning.l10advisor.AdvisorRagContractTest` | `com.example.ailearning.l10advisor.AdvisorRagOpenAiLiveTest` |
| 11 평가·실패 | `11-evaluation` | `com.example.ailearning.l11evaluation.EvaluationContractTest` | `com.example.ailearning.l11evaluation.EvaluationOpenAiLiveTest` |
| 11-2 모델 평가 | `11-model-evaluation` | `com.example.ailearning.l11judge.EvaluationApiContractTest` | `com.example.ailearning.l11judge.EvaluationApiOpenAiLiveTest` |
| 11-4 관측 | `11-observability` | `com.example.ailearning.l11observe.ObservationContractTest` | `com.example.ailearning.l11observe.ObservationOpenAiLiveTest` |
| 12 상담 Agent | `12-shopping-agent` | `com.example.ailearning.l12shopping.ShoppingAgentContractTest` | `com.example.ailearning.l12shopping.ShoppingAgentOpenAiLiveTest` |

보충 문서와의 대응은 실제 교육 계획과 소스를 기준으로 확인합니다.

## 코드와 실행 설정

- Java 코드는 실습별 패키지와 모듈로 격리합니다. 미래 단원의 클래스나 다른 서비스를 선행 의존성으로 끌어오지 않습니다.
- 공통 Mock 서버·프로토콜 응답·결과 출력은 `Spring-AI-Agent-Learning/test-support`를 `testImplementation`으로만 참조합니다. 이 일반 Java 라이브러리는 Boot 애플리케이션 플러그인 적용 대상에서 제외합니다. `OpenAiStubServer`는 주소·수명과 `chat()`·`embeddings()` 연결을 담당하고 두 대역이 각 설정·기록을 관리합니다. 단원별 응답·도구 순서·벡터 값·assertion은 실습 테스트에 둡니다.
- 같은 기능의 설명을 나눈 소수업은 한 실행 모듈을 공유할 수 있습니다. 번호를 맞추기 위해 모듈을 불필요하게 복제하지 않습니다.
- 실제 실행은 저장소 루트 `.env`를 properties 설정 파일로 읽습니다. 파일 자체나 키 값을 출력하지 않습니다.
- 채팅은 `OPENAI_API_KEY`, `OPENAI_MODEL`, 임베딩은 `OPENAI_EMBEDDING_MODEL`을 사용합니다. 두 제공 업체 실습만 Anthropic 키·모델을 추가합니다.
- `bootRun`·`test`의 작업 디렉터리는 저장소 루트입니다. IntelliJ 직접 실행도 같은 Working directory를 사용합니다.
- 로컬 테스트는 `.env` 읽기를 끄고 더미 설정과 로컬 고정 HTTP 서버를 사용합니다.
- Live 테스트는 별도 클래스입니다. 태그·실행 허용 변수·별도 Live 태스크를 만들지 않습니다. 모든 학습 모듈은 전체 `test`에 Live도 포함됩니다.
- 테스트 메서드명은 읽기 쉬운 한글과 밑줄로 작성합니다. 케이스 이름과 실제 결과를 콘솔에 출력하고 `assertThat` 검증도 유지합니다.
- 기존 `testLogging { showStandardStreams = true }`로 Gradle 콘솔에서도 출력을 봅니다. 샘플을 위해 Gradle 태스크를 복잡하게 확장하지 않습니다.

## Spring AI 설명에서 확인할 동작

- 복습·원복 확인 명령은 로컬 테스트 클래스를 명시합니다. 전체 `test`에 Live 테스트가 포함되는지 위 대응표와 실제 빌드 설정에서 확인합니다.
- ChatClient의 객체 생성·요청 구성·실행 시점과 Advisor의 메시지 저장 정책은 사용 버전의 실제 소스와 대조합니다. 직접 메모리 구현을 기본 Advisor로 바꿀 때 실패한 요청의 저장 여부까지 같다고 가정하지 않습니다.
- HTTP 오류 코드 설명은 Controller·Service가 실제로 처리하는 예외 범위와 대조합니다. 채팅 실패 처리 안에 질문 임베딩·검색 실패도 포함되는지 따로 확인합니다.
- 실제 도구 선택·모델 호출 횟수는 응답 문장에서 추정하지 않습니다. 요청별 기록, 관측 지표, 로컬 테스트의 전송 메시지 등 확인 가능한 경로를 안내합니다. 고정 응답 테스트의 횟수와 실제 모델의 선택을 구분합니다.

## 검증 명령

아래 명령은 저장소 루트에서 실행합니다. 예시는 표의 로컬 클래스만 실행하며 실제 API를 호출하지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test --tests com.example.ailearning.FirstChatLocalMockModelTest :Spring-AI-Agent-Learning:labs:02-first-chat:bootJar
```

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-multiple-models:test --tests com.example.ailearning.l02multiple.MultipleProvidersContractTest :Spring-AI-Agent-Learning:labs:02-multiple-models:bootJar
```

다른 단원도 표의 모듈과 로컬 테스트 클래스 이름을 사용해 같은 형식으로 실행합니다.
전체 저장소의 `./gradlew test`를 로컬 검증의 기본 명령으로 사용하지 않습니다.
같은 모듈의 IDE 테스트와 CLI 테스트를 동시에 실행하지 않습니다.
새 모듈 추가·이동 후 IntelliJ에 Run 아이콘이 없으면 루트 Gradle 프로젝트를 다시 동기화합니다.

공통 도구 자체는 `./gradlew :Spring-AI-Agent-Learning:test-support:test`로 검증합니다. Live 테스트 코드는 Mock 서버를 사용하지 않으며, 애플리케이션 실행 의존성에는 공통 도구를 넣지 않습니다.

## 과정별 행동 평가

E05는 이 과정의 실행 설정을 확인하는 선택 사례입니다. 검증 명령·외부 호출 구분 기준을 바꿀 때 적용하며 [공통 평가 절차](common/evals/README.md)를 따릅니다.
아래는 평가자용 정보입니다. 평가 대상에게는 필요한 실행 설정과 사용자 요청만 제공하고 이 절의 합격 기준은 제공하지 않습니다.

| 항목 | 내용 |
|---|---|
| ID·유형 | E05 · 외부 서비스 없는 로컬 검증 방법 선택 |
| 입력 준비 | 이 파일의 단원·실행 설정·검증 명령 절과 첫 호출 모듈의 build.gradle을 필요한 부분만 발췌해 제공. 이 평가 절·키·실제 서버는 제공하지 않음 |
| 사용자 요청 | 첫 호출 실습을 외부 서비스에 연결하지 않고 검증하는 테스트 명령을 알려줘. 실행하지는 마. |
| 필수 합격 기준 | 제공한 설정에서 저장소 루트와 `02-first-chat`의 `FirstChatLocalMockModelTest` 클래스 필터를 확인해 명령 제시. Live가 포함된 전체 test를 권장하거나 실제 실행하지 않음. 설정·소스 수정 없음 |

실제 명령·전체 클래스 이름의 기준은 위 검증 명령과 대응표입니다. 이 사례를 다른 과정에 가져갈 때 해당 과정의 설정으로 교체합니다.

## 기준 정보와 검사 도구

- 단원·실습 대응의 기준은 [교육 계획](../Spring-AI-Agent-Learning/docs/curriculum.md)입니다. 이 설정의 표는 테스트 클래스·Live 포함 여부·검증 명령을 관리합니다. 모듈 이동 시 교육 계획과 이 표를 함께 확인합니다.
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

- 기존 실습의 검증 근거는 [검증 기록](../Spring-AI-Agent-Learning/docs/verification.md)에 남깁니다.
- 하네스 자체의 적용·검토 결과는 `education-harness/reports/`에 둡니다. 제작 이력을 학습 본문에 삽입하지 않습니다.
- 단원 구현은 해당 테스트·빌드 완료 후 커밋합니다. 문서 수정은 변경 범위에 맞는 검사를 수행합니다.
- 외부 모델의 실제 품질은 로컬 고정 응답 테스트로 보장하지 않습니다. Live 실행 여부와 한계를 따로 기록합니다.
- 사용량 기준으로 추가 단원 작성 중단을 요청받았다면 확인 가능한 지표와 사용자와 합의한 기준을 사용합니다. 일일 수치가 없는데 다른 지표를 일일 수치로 표현하지 않습니다.
