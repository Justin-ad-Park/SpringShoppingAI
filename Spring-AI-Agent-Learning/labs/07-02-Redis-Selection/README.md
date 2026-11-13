# YAML로 Redis 또는 InMemory 선택

[저장소 연결 설명](../../docs/lessons/07-chat-memory/02-RepositorySelection.md)을 읽고 실행합니다.
이 모듈은 독립 Spring Boot 채팅 앱입니다. 07-00이나 다른 저장소 예제를 import하지 않습니다.


## 채팅 처리의 역할 구분

[MemoryChatService](src/main/java/com/example/ailearning/l07selection/MemoryChatService.java)의 `chat()`은 처리 순서를 조정합니다. 각 단계는 같은 클래스의 별도 메서드로 나눕니다.

| 메서드 | 역할 |
|---|---|
| `createRequestMessages(...)` | system 메시지·이전 대화·현재 질문을 순서대로 조합 |
| `callModel(...)` | 메시지 목록을 모델에 보내고 답변 반환 |
| `validateAnswer(...)` | null·공백 답변 거부 |
| `saveConversationTurn(...)` | 현재 질문과 답변을 함께 저장 |

모델 호출이나 응답 검증이 실패하면 저장 메서드를 호출하지 않습니다. `synchronized`는 기존처럼 `chat()` 전체에 적용하며, 조회부터 저장까지 단일 서버에서 순차 처리합니다.

## 먼저 읽을 코드

1. `src/main/resources/application.yml`: 저장 위치·연결·메시지 상한
2. `src/main/java/com/example/ailearning/l07selection/MemoryConfiguration.java`: 저장소를 ChatMemory에 연결
3. `MemoryChatService`: 이전 메시지 조회 → 모델 호출 → 성공한 질문·답변 저장
4. `MemoryController`: 서버 세션별로 대화를 구분하는 HTTP 진입점

테스트에 저장소 생성·선택 로직을 넣지 않습니다. 실제 앱 설정을 올리고 저장·조회·실패 뒤 상태만 검사합니다.
고정 설정은 `src/test/resources/application-test.yml`, 임의 포트로 시작하는 모델 대역의 주소만 `@DynamicPropertySource`에 둡니다.

## 실행과 확인

새 모듈이 IntelliJ에 보이지 않으면 루트 Gradle 프로젝트를 다시 동기화합니다. 모든 명령은 저장소 루트 기준입니다. 채팅 실행에는 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`이 필요하며 모델 API 사용량이 발생합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:07-02-Redis-Selection:bootRun
```

다른 터미널에서 같은 쿠키로 두 번 요청합니다.

```bash
curl -sS -c /tmp/l07selection.cookies -b /tmp/l07selection.cookies \
  http://127.0.0.1:8108/api/memory/demo \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '예산은 3만원이에요.'
curl -sS -c /tmp/l07selection.cookies -b /tmp/l07selection.cookies \
  http://127.0.0.1:8108/api/memory/demo \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '내 예산이 얼마인가요?'
curl -sS -b /tmp/l07selection.cookies http://127.0.0.1:8108/api/memory/demo
```

모델 문장은 달라질 수 있습니다. GET 결과에 이전 질문·답변과 현재 질문·답변이 있는지 확인합니다.
삭제는 같은 쿠키로 `DELETE /api/memory/demo`를 요청합니다. 서버 종료는 Ctrl+C입니다.
메시지 상한은 6개이고 성공한 질문·답변만 저장합니다. 모델 실패나 빈 응답은 새 대화로 저장하지 않습니다.

## 외부 모델 없이 검증

```bash
./gradlew :Spring-AI-Agent-Learning:labs:07-02-Redis-Selection:test --tests '*InMemorySelectionLocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:07-02-Redis-Selection:bootJar
```

## 저장소 선택

기본 `learning.memory.storage`는 `in-memory`입니다. Redis가 없어도 시작하며 Redis 클라이언트도 만들지 않습니다.
Redis는 같은 앱을 아래처럼 다시 시작해 선택합니다.

```bash
CHAT_MEMORY_REDIS_URL=redis://127.0.0.1:16379 ./gradlew :Spring-AI-Agent-Learning:labs:07-02-Redis-Selection:bootRun --args='--spring.profiles.active=redis'
```

또는 환경 변수 `CHAT_MEMORY_STORAGE=redis`로 선택합니다. `MemoryProperties`가 YAML 값을 타입으로 받고, `@ConditionalOnProperty`가 해당 저장소 빈만 등록합니다. 지원하지 않는 이름은 시작 실패로 드러납니다.
Redis 연결 실패를 InMemory로 숨겨 전환하지 않습니다. 저장소 변경은 재시작이 필요하며 기존 대화를 자동 복사하지 않습니다.

Redis URI는 환경 변수로 전달하고 실제 인증 값은 코드·로그에 넣지 않습니다. 인증·TLS 환경은 해당 Redis의 `redis://` 또는 `rediss://` URI를 사용합니다. 인덱스·키 접두사·TTL은 YAML에서 관리하며 기본 TTL은 24시간입니다. 운영에서 스키마를 별도 준비한다면 `CHAT_MEMORY_REDIS_INITIALIZE_SCHEMA=false`로 설정합니다.

### 실제 Redis Stack 테스트

Spring AI 2.0.2 저장소는 RedisJSON과 검색 기능이 필요합니다. 일반 Redis 서버만으로 실행된다고 가정하지 않습니다.
기존 Docker Desktop을 실행한 뒤 [runShell 실행·종료 안내](runShell/README.md)의 스크립트를 사용합니다. 기본 Redis 포트는 16379입니다. 실행 스크립트는 준비 완료까지 기다리고, 반복 실행 시 기존 컨테이너와 데이터 볼륨을 재사용합니다.

```bash
bash Spring-AI-Agent-Learning/labs/07-02-Redis-Selection/runShell/start-redis.sh
CHAT_MEMORY_REDIS_URL=redis://127.0.0.1:16379 ./gradlew :Spring-AI-Agent-Learning:labs:07-02-Redis-Selection:test --tests '*RedisMemoryRepositoryLiveTest'
bash Spring-AI-Agent-Learning/labs/07-02-Redis-Selection/runShell/stop-redis.sh
```

이 Live 테스트는 `test,redis` 프로필의 실제 앱 구성을 사용하고 OpenAI는 호출하지 않습니다. 실제 Redis 선택과 저장·조회·삭제를 검사하며, 테스트가 만든 임의 대화 ID만 정리합니다. 종료 스크립트는 컨테이너만 중지하고 데이터 볼륨은 보존합니다. 실습용 AOF 기록과 볼륨을 사용하며, 전체 상담 이력의 보관·삭제 정책은 별도 설계합니다.
InMemory 로컬 테스트는 Redis 빈이 생성되지 않는지와 모델 실패 시 대화 보존도 확인합니다. 전체 `test`에는 실제 Redis Live 테스트가 포함되므로 로컬 검증에는 클래스 필터를 사용합니다.

Jedis는 Spring AI 2.0.2의 RedisClient API와 맞는 7.4.1로 명시했습니다. 버전 갱신 시 Spring AI·Boot의 관리 버전과 함께 확인합니다.

## 상용 구성에 적용할 때

이 샘플은 서버 세션으로 소유자를 구분합니다. 대화 키는 세션 ID와 대화 ID를 UUID로 변환해 JDBC 기본 컬럼 길이 36자에 맞춥니다. 이는 로그인 인증이나 접근 권한 검사를 대신하지 않습니다.
서버 재시작으로 세션이 바뀌면 같은 브라우저의 저장된 대화를 자동 복구하지 못합니다. 실제 로그인 사용자에게 재연결하려면 안정적인 사용자 ID와 소유권 확인이 필요합니다.
최근 6개 메시지의 창과 화면에 표시할 전체 대화 이력은 분리하고, 다중 서버의 대화별 갱신 충돌도 별도 설계합니다. 운영 설계는 [07-5](../../docs/lessons/07-chat-memory/05-production-memory.md)를 참고합니다.
