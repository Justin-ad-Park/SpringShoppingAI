# 04. 대화 기억·window·JDBC·업무 상태

선수 단원: 01, 02.

## 읽는 순서

ConversationMemoryLesson → MessageWindowLesson → JdbcMemoryLesson → TaskStateLesson → SearchCriteriaReducer 순서로 읽는다.

1. 아래 핵심 Java 파일의 해당 실습 메소드를 읽는다.
2. 호출하는 도구·설정 파일을 따라간다.
3. 데이터가 궁금할 때만 support를 연다.
4. 아래 단원 테스트를 실행하고 질문·옵션을 바꿔 본다.

### 핵심 파일

- [ConversationMemoryLesson.java](ConversationMemoryLesson.java)
- [JdbcMemoryLesson.java](JdbcMemoryLesson.java)
- [MessageWindowLesson.java](MessageWindowLesson.java)
- [SearchCriteriaReducer.java](SearchCriteriaReducer.java)
- [TaskStateLesson.java](TaskStateLesson.java)

### support의 역할

- [DemoConversationMessages.java](support/DemoConversationMessages.java): window 비교에 쓸 고정 대화 4턴.
- [LearningDatabaseFactory.java](support/LearningDatabaseFactory.java): 독립 H2와 JDBC memory 스키마 준비.

### 테스트

- [ConversationMemoryLessonTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l04memory/ConversationMemoryLessonTest.java)

저장소 루트에서 실행:

```bash
./gradlew :Spring-AI-Tutor:test --tests 'com.example.shopping.tutor.lesson.l04memory.*'
```

태그가 붙은 LIVE/MCP 테스트는 전용 scripts/test_live.sh, test_mcp.sh, test_live_mcp.sh로 실행한다.

## 웹 실습

| caseId | 학습 목표 | 예시 질문 |
|---|---|---|
| L04-01 | 대화 기억 비교 | 예산 3만원이야 |
| L04-02 | 대화 격리와 초기화 | 나는 음료를 찾고 있어 |
| L04-03 | 메시지 window | 최근 메시지만 기억해줘 |
| L04-04 | JDBC memory 저장소 | 저장소를 다시 만들어도 대화가 남을까? |
| L04-05 | 대화 원문과 검색 상태 | 3만원 이하 음료 찾아줘 |

## 관찰과 변경 실험

같은 대화에서 예산을 정하고 후속 질문을 한다. 예산 해제와 미입력의 차이, 초기화 후 기록 제거를 확인한다.

학습 메소드는 필요한 Spring AI 객체와 일반 입력만 받는다. caseId 연결, 요청 검증, 세션 격리, 실패 턴 보호는 platform의 책임이다.
