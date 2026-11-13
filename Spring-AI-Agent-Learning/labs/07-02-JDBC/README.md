# JDBC로 대화 메모리 저장

[저장소 연결 설명](../../docs/lessons/07-chat-memory/02-jdbcRepository.md)을 읽고 실행합니다.
이 모듈은 독립 Spring Boot 채팅 앱입니다. 07-00이나 다른 저장소 예제를 import하지 않습니다.


## 채팅 처리의 역할 구분

[MemoryChatService](src/main/java/com/example/ailearning/l07jdbc/MemoryChatService.java)의 `chat()`은 처리 순서를 조정합니다. 각 단계는 같은 클래스의 별도 메서드로 나눕니다.

| 메서드 | 역할 |
|---|---|
| `createRequestMessages(...)` | system 메시지·이전 대화·현재 질문을 순서대로 조합 |
| `callModel(...)` | 메시지 목록을 모델에 보내고 답변 반환 |
| `validateAnswer(...)` | null·공백 답변 거부 |
| `saveConversationTurn(...)` | 현재 질문과 답변을 함께 저장 |

모델 호출이나 응답 검증이 실패하면 저장 메서드를 호출하지 않습니다. `synchronized`는 기존처럼 `chat()` 전체에 적용하며, 조회부터 저장까지 단일 서버에서 순차 처리합니다.

## 먼저 읽을 코드

1. `src/main/resources/application.yml`: 저장 위치·연결·메시지 상한
2. `src/main/java/com/example/ailearning/l07jdbc/JdbcMemoryConfiguration.java`: 저장소를 ChatMemory에 연결
3. `MemoryChatService`: 이전 메시지 조회 → 모델 호출 → 성공한 질문·답변 저장
4. `MemoryController`: 서버 세션별로 대화를 구분하는 HTTP 진입점

테스트에 저장소 생성·선택 로직을 넣지 않습니다. 실제 앱 설정을 올리고 저장·조회·실패 뒤 상태만 검사합니다.
고정 설정은 `src/test/resources/application-test.yml`, 임의 포트로 시작하는 모델 대역의 주소만 `@DynamicPropertySource`에 둡니다.

## 실행과 확인

새 모듈이 IntelliJ에 보이지 않으면 루트 Gradle 프로젝트를 다시 동기화합니다. 모든 명령은 저장소 루트 기준입니다. 채팅 실행에는 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`이 필요하며 모델 API 사용량이 발생합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:07-02-JDBC:bootRun
```

다른 터미널에서 같은 쿠키로 두 번 요청합니다.

```bash
curl -sS -c /tmp/l07jdbc.cookies -b /tmp/l07jdbc.cookies \
  http://127.0.0.1:8107/api/memory/demo \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '예산은 3만원이에요.'
curl -sS -c /tmp/l07jdbc.cookies -b /tmp/l07jdbc.cookies \
  http://127.0.0.1:8107/api/memory/demo \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '내 예산이 얼마인가요?'
curl -sS -b /tmp/l07jdbc.cookies http://127.0.0.1:8107/api/memory/demo
```

모델 문장은 달라질 수 있습니다. GET 결과에 이전 질문·답변과 현재 질문·답변이 있는지 확인합니다.
삭제는 같은 쿠키로 `DELETE /api/memory/demo`를 요청합니다. 서버 종료는 Ctrl+C입니다.
메시지 상한은 6개이고 성공한 질문·답변만 저장합니다. 모델 실패나 빈 응답은 새 대화로 저장하지 않습니다.

## 외부 모델 없이 검증

```bash
./gradlew :Spring-AI-Agent-Learning:labs:07-02-JDBC:test --tests '*JdbcMemoryLocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:07-02-JDBC:bootJar
```

## JDBC 설정과 보존 범위

기본 JDBC URL은 `jdbc:hsqldb:file:./data/learning07-jdbc/chat-memory`입니다. 채팅 시 실제 SQL로 저장하며 파일 DB는 저장소 루트의 `data/`에 생성됩니다. 이 폴더는 Git에서 제외합니다.
다른 DB는 `CHAT_MEMORY_JDBC_URL`, `CHAT_MEMORY_JDBC_USERNAME`, `CHAT_MEMORY_JDBC_PASSWORD`로 지정합니다. 예제에는 HSQLDB와 PostgreSQL 드라이버가 있으며, 다른 DB에는 해당 드라이버를 추가해야 합니다.

`spring.datasource` → DataSource·JdbcTemplate 자동 구성 → JdbcChatMemoryRepository → JdbcMemoryConfiguration의 ChatMemory 순서로 연결됩니다.
`learning.memory.max-messages`는 창 크기이며 DB 종류와 별개입니다.

스키마 생성은 실습 편의를 위해 기본 `always`입니다. 운영에서 마이그레이션으로 관리한다면 테이블과 인덱스를 먼저 준비하고 `CHAT_MEMORY_INITIALIZE_SCHEMA=never`로 시작합니다. 여러 서버의 동시 업데이트는 이 예제의 `synchronized`로 제어되지 않습니다.

로컬 테스트는 `application-test.yml`의 내장 메모리 DB를 사용합니다. 실제 SQL 행 저장, 새 메모리 객체에서 재조회, 이전 대화 재전달, 모델 실패 시 기존 대화 보존, 긴 대화 ID 처리를 검사합니다. 파일 DB의 프로세스 재시작은 이 테스트로 검증하지 않습니다.

## 상용 구성에 적용할 때

이 샘플은 서버 세션으로 소유자를 구분합니다. 대화 키는 세션 ID와 대화 ID를 UUID로 변환해 JDBC 기본 컬럼 길이 36자에 맞춥니다. 이는 로그인 인증이나 접근 권한 검사를 대신하지 않습니다.
서버 재시작으로 세션이 바뀌면 같은 브라우저의 저장된 대화를 자동 복구하지 못합니다. 실제 로그인 사용자에게 재연결하려면 안정적인 사용자 ID와 소유권 확인이 필요합니다.
최근 6개 메시지의 창과 화면에 표시할 전체 대화 이력은 분리하고, 다중 서버의 대화별 갱신 충돌도 별도 설계합니다. 운영 설계는 [07-6](../../docs/lessons/07-chat-memory/06-production-memory.md)를 참고합니다.
