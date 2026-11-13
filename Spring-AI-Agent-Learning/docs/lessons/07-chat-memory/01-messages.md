# 07-1. 이전 대화와 저장소 생성·연결

**실행 코드·설정·테스트:** [labs/07-00-chat-memory](../../../labs/07-00-chat-memory/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 이전 대화 재전달의 원리와 기본 저장소의 생성 위치를 이해합니다. 선택 예제에서는 JDBC 자동 구성과 InMemory·Redis 선택을 연결합니다.

**코드 읽기 안내:** 먼저 메시지 순서와 빌더 소스를 읽습니다. JDBC·Redis는 선택 예제로, 필요한 저장소 한 가지씩 실행한 뒤 다음으로 넘어갑니다.

## “기억한다”는 말의 실제 의미

- 첫 요청: “배송비 포함 예산은 3만원이야.”
- 다음 요청: “그 예산 안에서 음료로 추천해줘.”
- 두 번째 문장만 보내면 모델은 “그 예산”이 얼마인지 알 수 없습니다.
- 애플리케이션이 첫 질문과 답변을 저장하고, 두 번째 호출의 프롬프트에 함께 넣어야 합니다.
- 모델의 가중치를 다시 학습시키거나 이전 HTTP 연결 자체에 기억을 저장하는 것은 아닙니다.

이번 프롬프트는 다음 순서입니다.

| 순서 | 역할 | 내용 예 |
|---|---|---|
| 1 | system | 전달된 이전 대화와 현재 질문을 참고하라는 원칙 |
| 2 | user | 배송비 포함 예산은 3만원이야. |
| 3 | assistant | 네, 총예산 3만원을 참고하겠습니다. |
| 4 | user | 그 예산 안에서 음료로 추천해줘. |

assistant 문장은 **설명용 예시**입니다. 실제 모델의 문장은 달라집니다.

## 새 용어와 객체 관계

- `ChatMemory`: 대화 ID로 메시지를 저장·조회·삭제하는 Spring AI 인터페이스입니다.
- `MessageWindowChatMemory`: 최근 일정 개수의 메시지를 유지하는 Spring AI(2.0.2)의 유일한 구현체입니다. 여기서는 6개로 설정합니다.
- `ChatMemoryRepository`: 메시지를 저장·조회하는 인터페이스입니다. 어떤 메시지를 남길지 정하는 정책과 저장 위치를 분리합니다.
- `InMemoryChatMemoryRepository`: 대화별 메시지를 JVM 메모리에 저장하는 구현체입니다.
- 대화 ID(conversation ID): 같은 대화를 묶는 이름입니다. 하나의 사용자에게도 여러 대화가 있을 수 있습니다.
- 서버 세션(HttpSession): 브라우저 쿠키로 이어지는 서버 측 방문자 구분입니다.
  - 이 학습용 앱에서는 소유자 구분에 사용하며 로그인 인증을 구현한 것은 아닙니다.

```mermaid
classDiagram
    class MemoryController
    class MemoryChatService {
        +chat(owner, conversationId, question) MemoryReply
        +history(owner, conversationId) List
        +clear(owner, conversationId) void
    }
    class ChatMemory {
        <<interface>>
        +get(conversationId) List
        +add(conversationId, messages) void
        +clear(conversationId) void
    }
    class MessageWindowChatMemory
    class ChatMemoryRepository {
        <<interface>>
    }
    class InMemoryChatMemoryRepository
    MemoryController --> MemoryChatService : 세션 ID와 대화 ID 전달
    MemoryChatService --> ChatMemory : 직접 조회와 저장
    MessageWindowChatMemory ..|> ChatMemory : 구현
    MessageWindowChatMemory --> ChatMemoryRepository : 저장과 조회 위임
    InMemoryChatMemoryRepository ..|> ChatMemoryRepository : 구현
```

- 저장소 기본 구현은 프로세스 메모리를 사용합니다. 서버를 종료하면 사라집니다.
- ChatMemory는 모델에 보낼 문맥 관리용이며 전체 대화 영구 기록과 다릅니다.

## Spring AI가 제공하는 대화 저장소 종류

**보관 정책과 저장소 종류를 구분해서 읽습니다.** `MessageWindowChatMemory`는 최근 메시지를 유지하는 `ChatMemory` 구현체이고, 아래 여섯 가지는 메시지를 실제로 보관하는 **`ChatMemoryRepository` 구현체**입니다. 저장소를 바꾸면서 같은 메시지 창 정책을 사용할 수 있습니다.

이 교재가 사용하는 **Spring AI 2.0.2 기준**의 목록입니다.

| 구현체 | 저장 위치 | 알아둘 특징 |
|---|---|---|
| `InMemoryChatMemoryRepository` | JVM 메모리 | 기본 저장소. 프로세스 종료 시 사라지고 서버 간 자동 공유되지 않음 |
| `JdbcChatMemoryRepository` | PostgreSQL·MySQL 등 관계형 DB | JDBC로 메시지 저장. DB 연결·드라이버·스키마 준비 필요 |
| `CassandraChatMemoryRepository` | Cassandra | Cassandra에 대화 메시지를 보관 |
| `Neo4jChatMemoryRepository` | Neo4j | 그래프 DB에 메시지와 관계를 보관 |
| `MongoChatMemoryRepository` | MongoDB | 문서형 DB에 대화 메시지를 보관 |
| `RedisChatMemoryRepository` | Redis Stack | Redis Query Engine·RedisJSON이 필요하며 만료 시간(TTL) 설정 지원 |

InMemory 외의 저장소는 해당 의존성과 외부 저장소 연결 설정이 필요합니다. 여기서는 이름을 외우기보다 기존 인프라·보관 기간·메시지 지원 범위에 맞춰 저장 위치를 선택할 수 있다는 점을 이해합니다. 데이터가 재시작 후 유지되는지는 저장소의 영속화 설정까지 확인해야 합니다.

기본 실습은 InMemory를 사용하고, 이 문서의 선택 예제에서 JDBC 자동 구성과 InMemory·Redis 선택을 실행합니다. [07-4](04-production-memory.md)는 영구 이력·동시 요청 같은 운영 설계를 다룹니다. 다른 저장소를 학습 순서대로 모두 설치할 필요는 없습니다.

별도로 Azure Cosmos DB 팀이 관리하는 외부 모듈의 `CosmosDBChatMemoryRepository`도 있습니다. 위의 Spring AI 기본 제공 구현체와 구분합니다. 출처와 저장소별 설정은 [공식 Chat Memory의 Memory Storage 절](https://docs.spring.io/spring-ai/reference/api/chat-memory.html#_memory_storage)에서 확인합니다. 필요한 경우 인터페이스를 직접 구현해 다른 저장소에 연결할 수도 있습니다.

## 생성 코드에서 기본 저장소가 만들어지는 위치 찾기

기본 실습의 `MemoryChatService`는 다음처럼 메모리를 만듭니다. 저장소를 전달하는 줄은 없습니다.

```java
private final ChatMemory conversationMemory = MessageWindowChatMemory.builder()
        .maxMessages(6)
        .build();
```

### Spring AI 빌더의 실제 코드

다음은 **Spring AI 2.0.2의 `MessageWindowChatMemory` 소스 중 기본 개수 상수와 빌더 부분 발췌**입니다. 들여쓰기만 바꾸었습니다. 출처: [Spring AI 소스](https://github.com/spring-projects/spring-ai/blob/v2.0.2/spring-ai-model/src/main/java/org/springframework/ai/chat/memory/MessageWindowChatMemory.java), Spring 프로젝트 기여자, Apache License 2.0.

```java
private static final int DEFAULT_MAX_MESSAGES = 20;

public static Builder builder() {
    return new Builder();
}

public static final class Builder {

    private ChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();

    private int maxMessages = DEFAULT_MAX_MESSAGES;

    private Builder() {
    }

    public Builder chatMemoryRepository(ChatMemoryRepository chatMemoryRepository) {
        this.chatMemoryRepository = chatMemoryRepository;
        return this;
    }

    public Builder maxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
        return this;
    }

    public MessageWindowChatMemory build() {
        return new MessageWindowChatMemory(this.chatMemoryRepository, this.maxMessages);
    }

}
```

호출을 위 코드에 대입하면 다음 순서입니다.

1. `builder()`가 `new Builder()`를 실행합니다.
2. `Builder` 객체를 만들면서 필드의 **`new InMemoryChatMemoryRepository()`가 실행**됩니다. 이때 이미 기본 저장소가 생깁니다.
3. `.maxMessages(6)`은 개수만 바꿉니다. 저장소 필드는 그대로입니다.
4. `.build()`가 저장소 필드와 개수를 `MessageWindowChatMemory` 생성자에 전달합니다.

따라서 저장소 지정 생략은 **빌더가 이미 준비한 InMemory 저장소를 사용하는 것**입니다. Spring이 등록된 빈을 찾아서 넣는 과정은 이 코드에 없습니다.

직접 저장소를 연결하려면 다음처럼 전달합니다. `.chatMemoryRepository(repository)`는 빌더의 저장소 필드를 전달받은 객체로 바꿉니다.

```java
ChatMemoryRepository repository = new InMemoryChatMemoryRepository();
ChatMemory memory = MessageWindowChatMemory.builder()
        .chatMemoryRepository(repository)
        .maxMessages(6)
        .build();
```

서로 다른 빌더의 기본 저장소는 별개입니다. 반대로 같은 `repository`를 두 메모리에 전달하면 같은 저장소를 공유합니다. [MemoryBuilderUnitTest](../../../labs/07-00-chat-memory/src/test/java/com/example/ailearning/l07memory/MemoryBuilderUnitTest.java)가 두 경우의 저장·조회 결과를 비교합니다.

## JDBC 자동 구성: 독립 앱으로 저장소 연결하기

JDBC 예제는 [07-01-JDBC](../../../labs/07-01-JDBC/README.md)에서 실행합니다. 기본 `07-00-chat-memory`에는 JDBC 의존성이 없습니다.
먼저 [application.yml](../../../labs/07-01-JDBC/src/main/resources/application.yml)의 `spring.datasource`와 `spring.ai.chat.memory.repository.jdbc`를 읽습니다. 다음은 실제 YAML의 관련 항목 발췌입니다.

```yaml
spring:
  datasource:
    url: ${CHAT_MEMORY_JDBC_URL:jdbc:hsqldb:file:./data/learning07-jdbc/chat-memory}
    username: ${CHAT_MEMORY_JDBC_USERNAME:sa}
    password: ${CHAT_MEMORY_JDBC_PASSWORD:}
  ai:
    chat:
      memory:
        repository:
          jdbc:
            initialize-schema: ${CHAT_MEMORY_INITIALIZE_SCHEMA:always}
learning:
  memory:
    max-messages: 6
```

`${환경변수:기본값}`은 환경 변수로 값을 바꾸되 없으면 기본값을 사용하는 Spring 설정 문법입니다.
기본 주소의 `file:`은 파일 DB입니다. 로컬 테스트만 별도의 `application-test.yml`에서 `mem:` 주소로 바꿉니다.
JDBC Starter는 DataSource → JdbcTemplate → JdbcChatMemoryRepository를 자동 구성합니다. 앱은 아래 설정에서 그 저장소를 받아 보관 정책과 연결합니다.

[JdbcMemoryConfiguration.java](../../../labs/07-01-JDBC/src/main/java/com/example/ailearning/l07jdbc/JdbcMemoryConfiguration.java)의 전체 코드입니다.

```java
package com.example.ailearning.l07jdbc;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JdbcMemoryConfiguration {
    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository,
            @Value("${learning.memory.max-messages}") int maxMessages) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(maxMessages)
                .build();
    }
}
```

[MemoryChatService](../../../labs/07-01-JDBC/src/main/java/com/example/ailearning/l07jdbc/MemoryChatService.java)는 `ChatMemory`를 생성자로 주입받습니다. 필드에서 새 기본 저장소를 만들지 않습니다.
이전 메시지를 조회해 모델에 전달하고 성공한 질문·답변만 저장하는 흐름은 기본 실습과 같습니다. 접속 설정·생성 코드는 운영 소스에 있고 테스트는 그 구성을 사용합니다.

실행 명령과 SQL 저장 행·재조회 검증은 [JDBC README](../../../labs/07-01-JDBC/README.md)에 있습니다. 내장 DB 선택, 외부 DB 환경 변수, 스키마 초기화와 운영 마이그레이션의 차이도 함께 확인합니다.

## YAML로 Redis 또는 InMemory 선택하기

별도 앱 [07-02-Redis-Selection](../../../labs/07-02-Redis-Selection/README.md)은 **시작 시 저장소 하나를 선택**합니다. 기본값은 InMemory이고 Redis를 사용하려면 `redis` 프로필이나 환경 변수를 지정합니다.

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

위 코드는 [application.yml](../../../labs/07-02-Redis-Selection/src/main/resources/application.yml)의 실제 관련 부분입니다. `application-redis.yml`은 `storage: redis`로 바꿉니다.
`MemoryProperties`는 설정 값을 타입으로 받고, `@ConditionalOnProperty`는 지정한 값에 맞는 빈만 등록합니다.

| 선택 | 생성하는 저장소 | 실행 조건 |
|---|---|---|
| `in-memory` | `InMemoryChatMemoryRepository` | 외부 Redis 없이 실행. 서버 종료 시 소멸 |
| `redis` | `RedisChatMemoryRepository`와 Redis 클라이언트 | Redis Stack 연결 필요. TTL·Redis 영속화 설정에 따라 보존 |

[MemoryConfiguration.java](../../../labs/07-02-Redis-Selection/src/main/java/com/example/ailearning/l07selection/MemoryConfiguration.java)의 핵심은 다음과 같습니다. Redis의 연결·TTL 설정 부분은 전체 소스에서 읽습니다.

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

Redis 실행·Live 검증 명령은 [선택 예제 README](../../../labs/07-02-Redis-Selection/README.md)에 있습니다. 테스트에서 설정 클래스를 다시 조립하지 않고 실제 앱과 YAML을 사용합니다. 로컬 테스트는 InMemory 선택과 대화 재전달·실패 뒤 상태를, Redis Live 테스트는 실제 저장·조회·삭제를 검사합니다.

## 대화 의도로 실행 중 저장소를 선택하려면

[07-03-runtime-memory](../../../labs/07-03-runtime-memory/README.md)는 간단한 대화에는 InMemory, 상품 추천에는 Redis를 사용합니다. 두 메모리를 함께 준비하고, 의도 분류 결과를 `MemoryStorage` Enum으로 바꿔 `chat(storage, ...)`에 전달합니다.
07-02가 시작 시 한 저장소를 고르는 것과 달리, 07-03은 같은 실행 안에서 요청마다 고릅니다. 선택한 한 메모리에서 이전 대화를 조회하고 그 메모리에만 새 대화를 저장합니다. 실제 코드·고객 입력·두 저장소의 격리 검증은 해당 README에서 이어서 읽습니다.

## 서비스에서 사용하는 메서드

| 서비스에서 사용하는 메서드 | 의미 | 저장소에 위임하는 처리 |
|---|---|---|
| `add(conversationId, messages)` | 새 메시지를 추가하고, 보관 개수 상한을 넘으면 오래된 대화를 제거 | 기존 목록 조회 후 유지할 목록 저장 |
| `get(conversationId)` | 현재 보관된 메시지 조회 | `findByConversationId(...)` |
| `clear(conversationId)` | 해당 대화의 메모리 삭제 | `deleteByConversationId(...)` |

저장소의 `saveAll(conversationId, messages)`은 **그 대화의 기존 목록을 전달된 목록으로 교체**합니다. 새 메시지만 계속 추가하는 메서드가 아닙니다. 보통 서비스는 `ChatMemory.add(...)`를 사용하고, 저장소를 직접 갱신해 창 정책을 우회하지 않습니다.

`findConversationIds()`는 저장소가 가진 대화 ID 목록을 조회합니다. 현재 로그인 사용자가 읽어도 되는 대화를 걸러 주는 기능은 아니므로, 사용자용 대화 목록 API에는 별도 소유권 검사가 필요합니다.

## 메시지 창은 무엇을 유지할까?

`maxMessages(6)`은 이 객체가 관리하는 **각 대화의 보관 메시지 수 상한**입니다. 전체 사용자 수나 토큰 수를 제한하지 않습니다. 기본값은 20이며, 이 예제는 관찰하기 쉽게 6으로 지정했습니다.

2.0.2는 오래된 메시지를 줄일 때 사용자 질문으로 시작하는 대화 차례의 경계를 고려합니다. 그래서 항상 정확히 상한 개수가 남는 것은 아닙니다. 저장된 system 메시지는 특별히 보존하지만, 이번 서비스는 system을 매 요청에만 넣고 메모리에는 저장하지 않습니다. 이번처럼 질문 하나와 답변 하나씩 저장하면 최근 3회 대화를 관찰할 수 있습니다.

API 계약의 근거: [Spring AI 2.0.2의 메모리 소스](https://github.com/spring-projects/spring-ai/tree/v2.0.2/spring-ai-model/src/main/java/org/springframework/ai/chat/memory). 내부 구현을 외울 필요는 없으며 위의 역할 구분에 집중합니다.


## 확인 질문

1. 이전 대화를 저장만 하고 다음 프롬프트에 보내지 않으면 모델이 그 대화를 참고할 수 있나요?
2. JDBC 저장소 빈이 있는 앱에서 `MessageWindowChatMemory.builder().build()`를 직접 호출하면 그 JDBC 저장소를 자동으로 사용하나요?
3. YAML에서 저장 위치를 InMemory에서 Redis로 바꾸면 언제 적용되며, 이전 대화도 자동으로 옮겨지나요?

<details>
<summary>해설</summary>

1. 아닙니다. 같은 대화의 이전 user·assistant를 읽어 현재 질문과 함께 전달해야 합니다.
2. 아닙니다. 이 빌더는 자체 InMemory 저장소를 생성합니다. JDBC를 쓰려면 자동 구성된 저장소를 받아 `.chatMemoryRepository(jdbcRepository)`로 연결한 메모리를 사용해야 합니다.
3. 앱을 다시 시작할 때 적용됩니다. 이전 대화는 자동으로 복사되지 않으므로 저장소를 유지하거나 별도의 이전 절차를 설계해야 합니다.

</details>

[이전](../07-chat-memory.md) · [다음](02-store.md) · [단원 목차](../07-chat-memory.md) · [학습 방법](../../learning-guide.md)
