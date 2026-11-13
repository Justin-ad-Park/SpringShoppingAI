# 07-3. YAML로 Redis 또는 InMemory 선택하기

별도 앱 [07-03-Redis-Selection](../../../labs/07-03-Redis-Selection/README.md)은 **시작 시 저장소 하나를 선택**합니다. 기본값은 InMemory이고 Redis를 사용하려면 `redis` 프로필이나 환경 변수를 지정합니다.

```yaml
learning:
  memory:
    storage: ${CHAT_MEMORY_STORAGE:in-memory}
    max-messages: 6
    redis:
      uri: ${CHAT_MEMORY_REDIS_URL:redis://127.0.0.1:16379}
      index-name: ${CHAT_MEMORY_REDIS_INDEX:learning07-memory}
      key-prefix: ${CHAT_MEMORY_REDIS_PREFIX:learning07-memory:}
      time-to-live: 24h
      initialize-schema: ${CHAT_MEMORY_REDIS_INITIALIZE_SCHEMA:true}
```

위 코드는 [application.yml](../../../labs/07-03-Redis-Selection/src/main/resources/application.yml)의 실제 관련 부분입니다. `application-redis.yml`은 `storage: redis`로 바꿉니다.
`MemoryProperties`는 설정 값을 타입으로 받고, `@ConditionalOnProperty`는 지정한 값에 맞는 빈만 등록합니다.

| 선택 | 생성하는 저장소 | 실행 조건 |
|---|---|---|
| `in-memory` | `InMemoryChatMemoryRepository` | 외부 Redis 없이 실행. 서버 종료 시 소멸 |
| `redis` | `RedisChatMemoryRepository`와 Redis 클라이언트 | Redis Stack 연결 필요. TTL·Redis 영속화 설정에 따라 보존 |

[MemoryConfiguration.java](../../../labs/07-03-Redis-Selection/src/main/java/com/example/ailearning/l07selection/MemoryConfiguration.java)의 핵심은 다음과 같습니다. Redis의 연결·TTL 설정 부분은 전체 소스에서 읽습니다.

```java
@Bean
@ConditionalOnProperty(prefix = "learning.memory", name = "storage", havingValue = "in-memory")
public ChatMemoryRepository inMemoryRepository() {
    return new InMemoryChatMemoryRepository();
}

@Bean
public ChatMemory chatMemory(ChatMemoryRepository repository, MemoryProperties properties) {
    return MessageWindowChatMemory.builder()
            .chatMemoryRepository(repository)
            .maxMessages(properties.maxMessages())
            .build();
}
```

Redis 쪽 설정 클래스에도 `havingValue = "redis"` 조건이 있어 InMemory를 선택하면 Redis 클라이언트가 생성되지 않습니다. 선택한 저장소에 연결할 수 없으면 시작 실패로 드러내며 임시 메모리로 숨겨 전환하지 않습니다.

이 예제는 요청마다 두 저장소 사이를 바꾸는 라우팅을 구현하지 않습니다. 운영 환경별로 설정을 선택하는 예제이며, 변경 후에는 앱을 재시작합니다. 저장소를 바꾼다고 이전 대화가 자동 복사되지도 않습니다.

Redis 실행·Live 검증 명령은 [선택 예제 README](../../../labs/07-03-Redis-Selection/README.md)에 있습니다. 테스트에서 설정 클래스를 다시 조립하지 않고 실제 앱과 YAML을 사용합니다. 로컬 테스트는 InMemory 선택과 대화 재전달·실패 뒤 상태를, Redis Live 테스트는 실제 저장·조회·삭제를 검사합니다.

[기본 저장소 설명](01-messages.md) · [단원 목차](../07-chat-memory.md)

서비스의 메시지 구성·모델 호출·응답 검증·저장은 별도 메서드로 구분합니다. 처리 순서는 [기본 채팅 서비스 설명](04-store.md#chat은-처리-순서를-조정합니다), 해당 앱의 구성은 [실습 README](../../../labs/07-03-Redis-Selection/README.md#채팅-처리의-역할-구분)에서 확인합니다.
