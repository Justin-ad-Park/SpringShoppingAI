# 07-2. JDBC 자동 구성: 독립 앱으로 저장소 연결하기

JDBC 예제는 [07-02-JDBC](../../../labs/07-02-JDBC/README.md)에서 실행합니다. 기본 `07-00-chat-memory`에는 JDBC 의존성이 없습니다.
먼저 [application.yml](../../../labs/07-02-JDBC/src/main/resources/application.yml)의 `spring.datasource`와 `spring.ai.chat.memory.repository.jdbc`를 읽습니다. 다음은 실제 YAML의 관련 항목 발췌입니다.

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

[JdbcMemoryConfiguration.java](../../../labs/07-02-JDBC/src/main/java/com/example/ailearning/l07jdbc/JdbcMemoryConfiguration.java)의 전체 코드입니다.

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

[MemoryChatService](../../../labs/07-02-JDBC/src/main/java/com/example/ailearning/l07jdbc/MemoryChatService.java)는 `ChatMemory`를 생성자로 주입받습니다. 필드에서 새 기본 저장소를 만들지 않습니다.
이전 메시지를 조회해 모델에 전달하고 성공한 질문·답변만 저장하는 흐름은 기본 실습과 같습니다. 접속 설정·생성 코드는 운영 소스에 있고 테스트는 그 구성을 사용합니다.

실행 명령과 SQL 저장 행·재조회 검증은 [JDBC README](../../../labs/07-02-JDBC/README.md)에 있습니다. 내장 DB 선택, 외부 DB 환경 변수, 스키마 초기화와 운영 마이그레이션의 차이도 함께 확인합니다.

[기본 저장소 설명](01-messages.md) · [단원 목차](../07-chat-memory.md)

서비스의 메시지 구성·모델 호출·응답 검증·저장은 별도 메서드로 구분합니다. 처리 순서는 [기본 채팅 서비스 설명](04-store.md#chat은-처리-순서를-조정합니다), 해당 앱의 구성은 [실습 README](../../../labs/07-02-JDBC/README.md#채팅-처리의-역할-구분)에서 확인합니다.
