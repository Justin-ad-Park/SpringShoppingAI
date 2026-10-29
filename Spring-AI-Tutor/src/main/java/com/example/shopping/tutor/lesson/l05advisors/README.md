# 05. Advisor 순서와 메모리 조합

선수 단원: 01, 04.

## 읽는 순서

AdvisorOrderLesson 다음 TraceAdvisor의 before/after를 읽는다. 작은 order가 바깥쪽에서 요청을 감싼다.

1. 아래 핵심 Java 파일의 해당 실습 메소드를 읽는다.
2. 호출하는 도구·설정 파일을 따라간다.
3. 데이터가 궁금할 때만 support를 연다.
4. 아래 단원 테스트를 실행하고 질문·옵션을 바꿔 본다.

### 핵심 파일

- [AdvisorOrderLesson.java](AdvisorOrderLesson.java)
- [TraceAdvisor.java](TraceAdvisor.java)

### support의 역할

- [DiagnosticText.java](support/DiagnosticText.java): 전화번호·키를 화면에서 가리는 예제. 모델 전송 차단 기능은 아니다.

### 테스트

- [AdvisorOrderLessonTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l05advisors/AdvisorOrderLessonTest.java)

저장소 루트에서 실행:

```bash
./gradlew :Spring-AI-Tutor:test --tests 'com.example.shopping.tutor.lesson.l05advisors.*'
```

태그가 붙은 LIVE/MCP 테스트는 전용 scripts/test_live.sh, test_mcp.sh, test_live_mcp.sh로 실행한다.

## 웹 실습

| caseId | 학습 목표 | 예시 질문 |
|---|---|---|
| L05-01 | Advisor 호출 순서 | 선물 고르는 기준 알려줘 |
| L05-02 | 진단 출력 마스킹 | 010-1234-5678로 배송 문의하고 싶어 |
| L05-03 | Memory와 Trace 조합 | 내 예산은 3만원이야 |

## 관찰과 변경 실험

A/B order를 바꾸어 실행 순서를 비교한다. 전화번호 마스킹은 진단 표시용이며 provider 원문 전송 차단이 아님을 확인한다.

학습 메소드는 필요한 Spring AI 객체와 일반 입력만 받는다. caseId 연결, 요청 검증, 세션 격리, 실패 턴 보호는 platform의 책임이다.
