# 12. usage·지연·제한된 읽기 재시도

선수 단원: 01.

## 읽는 순서

OperationsLesson의 retryRead에서 재시도 대상 상태와 최대 횟수를 확인한다. STUB과 설정 모델을 같은 API로 비교한다.

1. 아래 핵심 Java 파일의 해당 실습 메소드를 읽는다.
2. 호출하는 도구·설정 파일을 따라간다.
3. 데이터가 궁금할 때만 support를 연다.
4. 아래 단원 테스트를 실행하고 질문·옵션을 바꿔 본다.

### 핵심 파일

- [OperationsLesson.java](OperationsLesson.java)

### support의 역할

이 단원에는 별도의 support가 필요하지 않다.

### 테스트

- [OperationsLessonTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l12operations/OperationsLessonTest.java)

저장소 루트에서 실행:

```bash
./gradlew :Spring-AI-Tutor:test --tests 'com.example.shopping.tutor.lesson.l12operations.*'
```

태그가 붙은 LIVE/MCP 테스트는 전용 scripts/test_live.sh, test_mcp.sh, test_live_mcp.sh로 실행한다.

## 웹 실습

| caseId | 학습 목표 | 예시 질문 |
|---|---|---|
| L12-01 | 사용량과 지연 관측 | 텀블러 선택 기준 알려줘 |
| L12-02 | 일시적 읽기 실패 재시도 | 503 다음 성공을 재현해줘 |
| L12-03 | 모델 계약 비교 | 선물 고르는 기준 알려줘 |

## 관찰과 변경 실험

401은 한 번, 503은 최대 두 번 시도하는 테스트를 읽는다. 변경 요청에는 이 재시도를 적용하지 않는다.

학습 메소드는 필요한 Spring AI 객체와 일반 입력만 받는다. caseId 연결, 요청 검증, 세션 격리, 실패 턴 보호는 platform의 책임이다.
