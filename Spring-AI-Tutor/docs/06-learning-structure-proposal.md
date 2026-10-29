# 학습하기 쉬운 Java 구조 개선안

상태: **제안**. Java 패키지 이동과 레슨 API 재설계는 아직 적용하지 않았다.

## 1. 현재 코드에서 어려운 지점

실행을 한곳에서 관리하도록 구현하면서 학습자가 알아야 할 공통 구조가 너무 커졌다.

- 모든 수업이 평평한 `lesson`에 있어 단원별 진입 파일이 드러나지 않는다.
- `RunContext`가 입력 검증, 모델 생성, 메모리, 도구 실행 제한, 진단, 상태 확정을 함께 다룬다. 첫 호출을 배우려는 사람도 이 객체를 따라가게 된다.
- `Shop` 안에 상품 모델, 검색 조건, fixture, 검색, 배송 계산, `@Tool`이 섞여 있다.
- `Knowledge.store()`에 실제 VectorStore 생성·적재가 숨고, `TraceAdvisor` 같은 학습 핵심 코드가 support에 있다.
- 수업이 다른 수업의 `Cases`를 직접 생성하므로 단원의 의존 관계를 예상하기 어렵다.
- 테스트가 큰 공통 클래스에 모여 있어 특정 수업의 구현과 테스트를 함께 찾기 어렵다.

따라서 **단원 패키지 + 단원 전용 support + 학습실 실행 기반 분리**를 권장한다. 파일 이동과 함께 레슨의 입력·의존성도 단순하게 바꿔야 한다.

## 2. 패키지 원칙

| 위치 | 넣을 코드 | 읽는 순서 |
|---|---|---|
| 단원 루트 | 해당 단원의 Spring AI 호출, 핵심 설정, 학습하는 Tool/Advisor | 필수 |
| 단원 하위 `support` | 데모 데이터, 실습 준비, 보조 변환 | 필요할 때 |
| `platform` | 웹 연결, 세션 격리, 실행 잠금, 관찰용 래퍼, 키 관리 | 처음에는 건너뜀 |

support라는 이유만으로 파일을 모두 아래로 내리지 않는다. **그 단원에서 배우는 코드가 무엇인지**가 기준이다. 예를 들어 `TraceAdvisor`와 `ProductSearchTools`는 학습 대상이므로 단원 루트에 둔다.

## 3. 권장 트리

```text
com.example.shopping.tutor
├── TutorApplication.java
├── lesson
│   ├── l00setup
│   ├── l01chat
│   │   ├── README.md                  # 소스 디렉터리에서 보는 학습 안내
│   │   ├── BasicChatLesson.java
│   │   └── PromptTemplateLesson.java
│   ├── l02structured
│   ├── l03streaming
│   ├── l04memory
│   │   ├── README.md
│   │   ├── ConversationMemoryLesson.java
│   │   ├── MessageWindowLesson.java
│   │   ├── JdbcMemoryLesson.java
│   │   ├── ChatMemoryConfiguration.java
│   │   └── support
│   │       ├── DemoConversationMessages.java
│   │       └── LearningDatabaseFactory.java
│   ├── l05advisors
│   │   ├── AdvisorOrderLesson.java
│   │   ├── TraceAdvisor.java           # 학습 대상: 루트에서 바로 찾는다
│   │   └── support
│   │       └── AdvisorExecutionLog.java
│   ├── l06tools
│   │   ├── ProductToolCallingLesson.java
│   │   ├── ProductSearchTools.java     # @Tool을 직접 읽는다
│   │   └── support
│   │       ├── DemoProductCatalog.java
│   │       └── DemoCartRepository.java
│   ├── l07vector
│   ├── l08etl
│   ├── l09rag
│   ├── l10mcp
│   ├── l11evaluation
│   ├── l12operations
│   ├── l13shopping
│   └── l14multimodal
└── platform
    ├── config                         # 키·실행 환경
    ├── web                            # Controller, catalog, 수업 연결
    ├── execution                      # 세션, 잠금, 실패 턴 보호
    ├── observation                    # 프롬프트·usage·도구 기록
    └── stub                           # 전체 학습실의 준비된 응답 모델
```

처음부터 모든 단원에 support를 만들 필요는 없다. 해당 단원의 준비 코드가 생길 때만 만든다. 실제 파일 이름은 아래처럼 구체적인 개념을 사용하고, `Helper`, `Utils`, `Manager`를 포괄적인 이름으로 쓰지 않는다.

## 4. 기존 support 파일을 어떻게 나눌까

| 현재 파일 | 권장 배치·개선 |
|---|---|
| `RunContext` | 입력·상태·관찰을 `platform`의 책임으로 분리. 기초 레슨 메소드에서는 제거 |
| `Models` | 키/URL은 `platform.config`, 모델 설정을 배우는 예제는 관련 단원에 명시적으로 제공 |
| `Outcome` | `platform.web`의 웹 응답 조립에 사용. 기초 레슨은 String/record/Flux 반환 |
| `TutorCase` | `platform.web`의 catalog 메타데이터. 처음부터 학생이 이해해야 하는 계약으로 만들지 않음 |
| `ScriptedChatModel` | `platform.stub`. STUB 구현 자체는 선택 읽기 자료로 연결 |
| `FixedEmbeddingModel` | `l07vector.support`의 교육용 임베딩 구현 |
| `TraceAdvisor` | `l05advisors` 루트. Advisor 수업의 핵심 소스 |
| `Shop` | 구조화 DTO는 02, 도구와 데모 저장소는 06, 통합 배송 정책은 13으로 책임별 분리 |
| `SandboxCart` | `l06tools.support`. 변경 확인 사례의 안내에서 해당 파일을 연결 |
| `Knowledge` | 문서 fixture와 출력 변환은 각 단원 support. VectorStore 생성·검색·필터는 07/09의 학습 코드로 꺼냄 |
| `PolicyMcpConfiguration` | `l10mcp` 루트의 서버 설정 예제 |
| `MediaModels` | `l14multimodal`의 이미지·음성·moderation 설정으로 분리 |

같은 상품 데이터가 필요하면 원본 fixture 리소스를 공유할 수 있다. 짧은 예제 DTO나 준비 코드는 단원의 독립적인 이해를 위해 소량 중복해도 된다. 단순히 중복을 없애려고 새로운 전역 공통 계층을 만들지 않는다.

## 5. 레슨 메소드는 학습 개념을 직접 보여준다

기초 수업에서 `RunContext`의 여러 필드를 사용하는 대신, 필요한 모델/메모리를 생성자에 받고 질문·대화 ID를 메소드 입력으로 받는다. 다음은 제안 예시이며 현재 구현된 클래스는 아니다.

```java
public final class ConversationMemoryLesson {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ConversationMemoryLesson(ChatModel chatModel, ChatMemory chatMemory) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.chatMemory = chatMemory;
    }

    /**
     * 이전 메시지를 함께 전달해 후속 질문에 답한다.
     * 같은 대화를 이어갈 때는 같은 conversationId를 전달한다.
     */
    public String chatWithMemory(String conversationId, String question) {
        // 1. 메모리를 모델 요청에 붙이는 Advisor를 준비한다.
        MessageChatMemoryAdvisor memoryAdvisor =
                MessageChatMemoryAdvisor.builder(chatMemory).build();

        // 2. 대화 식별자를 전달한다. ID의 사용자 격리는 웹 실행 계층에서 보장한다.
        return chatClient.prompt()
                .advisors(memoryAdvisor)
                .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID,
                        conversationId
                ))
                .user(question)
                .call()
                .content();
    }
}
```

실제 서버에서는 플랫폼이 현재 요청에 맞는 관찰 모델·staging memory를 이 클래스에 주입한다. 단순화 과정에서도 현재의 사용자 격리·실패 턴 미반영 동작을 잃지 않아야 한다. 모든 레슨을 위한 새 거대 입력 객체를 만들어 `RunContext` 문제를 반복하지 않는다.

## 6. 읽기 순서와 주석 규칙

각 단원의 README는 다음 5가지를 짧게 제공한다.

1. 배울 개념과 선수 단원.
2. `① 핵심 레슨 → ② 핵심 설정/도구 → ③ 필요 시 support → ④ 테스트` 순서.
3. 실행할 caseId와 예시 질문.
4. 요청·응답에서 관찰할 내용.
5. 정상·실패 실습과 다음 단원으로 이어지는 질문.

주석은 선언을 반복하는 대신 **왜 필요한지, 무엇을 바꿔 보면 되는지, 어떤 결과를 확인할지**를 설명한다. `// call 호출` 대신 `// 여기서 실제 모델 호출이 발생한다. STUB에서는 준비된 응답을 받는다.`처럼 쓴다. 모든 줄에 주석을 달지는 않는다.

코드는 100자 안팎을 기준으로 줄을 나누고, 한 줄에 한 동작만 쓴다. 체인은 호출 단계별로 줄바꿈한다. 긴 프롬프트는 text block을 사용한다. 중첩 삼항식, 거대한 Map 조립, 과도한 stream 중첩은 지역 변수·조건문·record로 풀어 쓴다. `c`, `p`, `t`보다 `question`, `product`, `toolCallback`처럼 역할이 드러나는 이름을 사용한다.

`Cases` 클래스 하나가 서로 다른 학습 목표를 너무 많이 담으면 `MessageWindowLesson`, `JdbcMemoryLesson`처럼 나눈다. 반대로 같은 목표의 짧은 비교 예제까지 기계적으로 파일 하나씩 늘리지는 않는다.

## 7. 테스트·웹 탐색도 함께 맞춘다

- 테스트는 `src/test/java`에서 동일한 단원 패키지를 따라 배치한다.
- 정상·실패 행위 검증은 `ConversationMemoryLessonTest`처럼 핵심 클래스 옆에 대응시킨다.
- 전체 caseId 누락 검사는 공통 catalog 테스트로 유지하되, 수업별 학습 테스트를 대신하지 않는다.
- 웹은 Java 소스 하나뿐 아니라 `필수 파일 / 참고 support / 테스트` 목록과 읽는 순서를 제공하도록 확장한다.
- catalog는 현재 정확히 `lesson` 패키지만 찾는다. 하위 패키지 탐색, 소스 경로, 테스트 경로 매핑도 함께 수정해야 한다.
- 13 통합 단원을 제외하고 다른 단원의 `Cases`를 `new`로 호출하지 않는다. 통합 단원에서도 어떤 기능을 재사용하는지 명시한다.

## 8. 적용 순서

먼저 01 ChatClient와 04 Memory 두 단원을 기준 형태로 정리한다. 이후 단원 이동, support 책임 분리, 테스트 이동, 웹 파일 안내 순으로 확장한다. 단계마다 기존 caseId, API, STUB/LIVE/MCP 실행, 메모리 격리, 취소와 실패 턴 보호를 회귀 검증한다.

이번 변경에서는 프런트엔드의 역할 분리와 테스트 실행 스크립트 정리를 적용했다. 위 Java 구조 제안은 별도의 리팩터링 범위다.
