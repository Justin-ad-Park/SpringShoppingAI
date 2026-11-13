# 실습 07: 대화 메모리

[교재와 쿠키 실험](../../docs/lessons/07-chat-memory.md)을 먼저 읽습니다.
읽는 순서: MessageView/MemoryReply → MemoryChatService → MemoryController.
이 모듈에는 도구·MCP·RAG·Advisor 연결이 없습니다. ChatMemory에서 읽은 이전 메시지를 직접 프롬프트에 담습니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:07-00-chat-memory:test --tests com.example.ailearning.l07memory.MemoryLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:07-00-chat-memory:bootRun
```

포트 8097. POST `/api/stateless`는 기억 없음, POST `/api/memory/demo`는 세션별 기억을 사용합니다.
GET/DELETE `/api/memory/demo`는 현재 세션의 해당 대화만 조회/삭제합니다. POST 본문은 text/plain입니다.
bootRun은 루트 `.env`의 OPENAI_API_KEY와 OPENAI_MODEL을 읽습니다. 로컬 테스트에는 실제 키가 필요 없습니다.
저장은 JVM 메모리뿐이며 서버 재시작 시 사라집니다. 저장 6개, 전송 최대 8개 메시지를 구분하세요.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다. 이 의존성은 테스트에만 적용됩니다. 각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `MemoryLocalModelTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `MemoryOpenAiLiveTest`: 이전 질문·첫 답변·현재 질문이 다음 요청에 순서대로 포함되는지 확인하고 실제 OpenAI에 연결합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 테스트 시작 시 빈 줄과 `===== [ 테스트 메서드명 ] ======`을 한 번 출력하고, 그 아래에서 응답·검증 대상 값을 확인합니다. 출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:07-00-chat-memory:test --tests com.example.ailearning.l07memory.MemoryLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:07-00-chat-memory:test --tests com.example.ailearning.l07memory.MemoryOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다. 클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.

## 선택 예제: 기본 저장소·JDBC 자동 구성·실행 중 Redis 선택

[07-1의 저장소 연결 예제](../../docs/lessons/07-chat-memory/01-messages.md)를 읽습니다. 예제는 `src/test/java/com/example/ailearning/l07repositories`에 있으며 기본 `MemoryChatService`와 별도 패키지입니다. JDBC와 Redis 라이브러리는 테스트 범위에서만 사용합니다.

| 읽을 코드 | 확인할 동작 |
|---|---|
| `MemoryBuilderUnitTest` | 기본 빌더의 저장소는 별개, 같은 저장소를 전달하면 대화 공유 |
| `JdbcMemoryConfiguration` → `JdbcMemoryRepositoryIntegrationTest` | 실제 HSQLDB·JDBC 자동 구성, 테이블 저장과 새 메모리에서 재조회 |
| `SelectableMemoryConfiguration` → `MemoryStorage` → `SelectableMemoryService` | 두 저장소를 명시적으로 생성·연결하고 Enum으로 요청마다 선택 |
| `RedisMemoryRepositoryLiveTest` | 실제 Redis Stack에서 저장·조회하고 InMemory와 대화 격리 확인 |

아래 명령은 실제 모델과 외부 Redis 없이 실행합니다. HSQLDB도 `mem:` 주소이므로 프로세스 재시작 후 영구 보존까지 확인하는 테스트는 아닙니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:07-00-chat-memory:test --tests com.example.ailearning.l07repositories.MemoryBuilderUnitTest --tests com.example.ailearning.l07repositories.JdbcMemoryRepositoryIntegrationTest --tests com.example.ailearning.l07memory.MemoryLocalModelTest
```

Redis 예제는 실행 중인 Redis Stack이 필요합니다. Docker가 실행된 환경에서:

```bash
docker run --rm -d --name learning07-redis -p 127.0.0.1:16379:6379 redis/redis-stack-server:7.4.0-v3
LEARNING_REDIS_PORT=16379 ./gradlew :Spring-AI-Agent-Learning:labs:07-00-chat-memory:test --tests com.example.ailearning.l07repositories.RedisMemoryRepositoryLiveTest
docker stop learning07-redis
```

컨테이너는 실습용으로 제거되며 모델 API를 호출하지 않습니다. 호스트·포트는 `LEARNING_REDIS_HOST`·`LEARNING_REDIS_PORT`로 변경할 수 있습니다. 다른 Redis에 연결할 때도 전용 `learning07-memory` 인덱스와 키 접두사를 사용하며 테스트가 만든 대화 ID만 지웁니다.

JDBC 예제와 두 저장소 예제는 각각 필요한 설정만 올립니다. 실제 앱으로 옮기려면 테스트 의존성을 앱 의존성으로 바꾸고, 채팅 서비스가 생성자에서 선택한 `ChatMemory`를 사용하게 연결합니다. 구현 순서는 교재를 따릅니다.
