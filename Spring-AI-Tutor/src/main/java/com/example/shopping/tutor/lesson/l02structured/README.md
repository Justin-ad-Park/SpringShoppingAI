# 02. JSON 응답을 검증된 record로 변환

선수 단원: 01.

## 읽는 순서

extract → entity(SearchCriteria.class) → record 생성자 순으로 읽는다. JSON 파싱 성공과 업무 유효성 검증은 별개다.

1. 아래 핵심 Java 파일의 해당 실습 메소드를 읽는다.
2. 호출하는 도구·설정 파일을 따라간다.
3. 데이터가 궁금할 때만 support를 연다.
4. 아래 단원 테스트를 실행하고 질문·옵션을 바꿔 본다.

### 핵심 파일

- [Recommendation.java](Recommendation.java)
- [SearchCriteria.java](SearchCriteria.java)
- [StructuredOutputLesson.java](StructuredOutputLesson.java)

### support의 역할

이 단원에는 별도의 support가 필요하지 않다.

### 테스트

- [StructuredOutputLessonTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l02structured/StructuredOutputLessonTest.java)

저장소 루트에서 실행:

```bash
./gradlew :Spring-AI-Tutor:test --tests 'com.example.shopping.tutor.lesson.l02structured.*'
```

태그가 붙은 LIVE/MCP 테스트는 전용 scripts/test_live.sh, test_mcp.sh, test_live_mcp.sh로 실행한다.

## 웹 실습

| caseId | 학습 목표 | 예시 질문 |
|---|---|---|
| L02-01 | 자연어를 record로 | 3만원 이하 생활용품 찾아줘 |
| L02-02 | 제네릭 추천 목록 | P004와 P005를 비교해줘 |
| L02-03 | 잘못된 JSON과 교정 | 3만원 이하 음료 찾아줘 |
| L02-04 | Native JSON Schema | 3만원 이하 생활용품 찾아줘 |

## 관찰과 변경 실험

STUB의 invalid-json 실험으로 한 번만 교정하는지 확인한다. 음수 예산과 중복 후보는 테스트에서 거부한다.

학습 메소드는 필요한 Spring AI 객체와 일반 입력만 받는다. caseId 연결, 요청 검증, 세션 격리, 실패 턴 보호는 platform의 책임이다.
