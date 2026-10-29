# 11. 결정적인 사실 검증·관련성 평가·회귀 질문

선수 단원: 02, 09.

## 읽는 순서

EvaluationLesson에서 Java 사실 검증과 RelevancyEvaluator 판정을 구분한다. 관련성 판정은 사실성 보장이 아니다.

1. 아래 핵심 Java 파일의 해당 실습 메소드를 읽는다.
2. 호출하는 도구·설정 파일을 따라간다.
3. 데이터가 궁금할 때만 support를 연다.
4. 아래 단원 테스트를 실행하고 질문·옵션을 바꿔 본다.

### 핵심 파일

- [EvaluationLesson.java](EvaluationLesson.java)

### support의 역할

이 단원에는 별도의 support가 필요하지 않다.

### 테스트

- [EvaluationLessonTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l11evaluation/EvaluationLessonTest.java)
- [LiveEvaluationTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l11evaluation/LiveEvaluationTest.java)

저장소 루트에서 실행:

```bash
./gradlew :Spring-AI-Tutor:test --tests 'com.example.shopping.tutor.lesson.l11evaluation.*'
```

태그가 붙은 LIVE/MCP 테스트는 전용 scripts/test_live.sh, test_mcp.sh, test_live_mcp.sh로 실행한다.

## 웹 실습

| caseId | 학습 목표 | 예시 질문 |
|---|---|---|
| L11-01 | 업무 사실 검증 | P004와 P005의 가격과 배송비 확인 |
| L11-02 | RelevancyEvaluator | 미개봉 반품은 7일 이내인가요? |
| L11-03 | 회귀 질문셋 | 검색 조건 질문셋 실행 |

## 관찰과 변경 실험

회귀 라벨을 바꾸어 질문별 pass가 바뀌는지 관찰한다. 실제 모델 검증은 test_live.sh를 실행한다.

학습 메소드는 필요한 Spring AI 객체와 일반 입력만 받는다. caseId 연결, 요청 검증, 세션 격리, 실패 턴 보호는 platform의 책임이다.
