# 04. 앞에서 말한 예산을 함께 보내 보세요

오늘은 [ConversationMemoryLesson.java](ConversationMemoryLesson.java)의
**chatWithMemory() 하나**를 먼저 읽습니다.

## 1. 첫 호출에서 무엇이 추가됐나요?

마지막 user(question), call(), content()는 첫 수업과 같습니다.
추가된 MessageChatMemoryAdvisor는 이전 대화를 이번 질문과 함께 넣습니다.
conversationId는 대화 이름입니다. 같은 대화를 이어갈 때 같은 값을 씁니다.
Advisor는 요청 전후에 일을 더하는 기능의 이름입니다. 지금은 “앞 대화를 붙여 준다”로 이해해도 됩니다.

## 2. 두 질문을 순서대로 보내기

웹 **L04-02**에서 다음을 하나씩 보내세요.
1. 예산은 3만원이야
2. 내 예산이 얼마였지?

두 번째 실행의 “AI에게 함께 보낸 이전 대화”에 첫 질문이 있나요?
STUB은 예산을 이해해 답하지 않습니다. 실제로 전달한 이전 대화를 확인하세요.
LIVE에서는 답변 내용도 함께 확인합니다.

## 3. 한 가지 바꾸기

[ConversationMemoryLessonTest](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l04memory/ConversationMemoryLessonTest.java)에서
두 번째 호출의 my-shopping을 other-shopping으로 바꾸세요.
이제 이전 질문도 함께 전달될까요? 먼저 예상하고 테스트를 실행하세요.
같은 대화 테스트는 실패해야 합니다. 다른 대화 테스트와 비교해 보세요.
저장소 루트에서 `./Spring-AI-Tutor/scripts/test_beginner.sh`를 실행하면 첫 호출·상품 검색·대화 기억 테스트를 함께 확인할 수 있습니다.

웹에서는 “새 대화”를 누른 뒤 두 번째 질문만 보내 같은 차이를 확인할 수 있습니다.

## 4. 테스트를 하나씩 읽기

- 같은_대화에서는_이전_질문이_함께_전달된다: 같은 대화 이름의 요청.
- 다른_대화에는_이전_질문을_전달하지_않는다: 다른 대화 이름의 요청.

QuestionRecordingModel은 실제 요청에 들어간 질문을 기록하는 연습용 모델입니다. 테스트에서 앞 질문이 포함됐는지 직접 확인합니다. 기록하는 방법은 테스트 폴더 support에 있으며 처음에는 읽지 않아도 됩니다.

## 이해한 다음 더 읽기

- L04-01: 앞 대화를 넣은 요청과 넣지 않은 요청 비교.
- [MessageWindowLesson](MessageWindowLesson.java) / [테스트](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l04memory/MessageWindowLessonTest.java): 최근 메시지 몇 개만 남기기.
- [JdbcMemoryLesson](JdbcMemoryLesson.java) / [테스트](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l04memory/JdbcMemoryLessonTest.java): DB에 저장했다가 읽기.
- [TaskStateLesson](TaskStateLesson.java) / [SearchCriteriaReducerTest](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l04memory/SearchCriteriaReducerTest.java): 예산 변경과 해제.
- [ConversationMemoryWebTest](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l04memory/ConversationMemoryWebTest.java): 웹에서 대화 기록이 섞이지 않는지 확인. 처음에는 건너뛰어도 됩니다.

support의 DemoConversationMessages는 준비한 대화, LearningDatabaseFactory는 연습 DB를 만드는 코드입니다.
지금은 필요하지 않으므로 window·DB 수업에서 읽으세요.

다음은 [09 안내문을 찾아 답하기](../l09rag/README.md)입니다.
