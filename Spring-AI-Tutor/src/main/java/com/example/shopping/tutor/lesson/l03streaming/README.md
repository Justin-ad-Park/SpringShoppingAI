# 03. Flux 구독·취소·부분 실패

선수 단원: 01.

## 읽는 순서

stream 메소드의 .stream().content()가 응답 조각을 내보낸다. 브라우저 정지는 서버 구독 취소로 이어진다.

1. 아래 핵심 Java 파일의 해당 실습 메소드를 읽는다.
2. 호출하는 도구·설정 파일을 따라간다.
3. 데이터가 궁금할 때만 support를 연다.
4. 아래 단원 테스트를 실행하고 질문·옵션을 바꿔 본다.

### 핵심 파일

- [StreamingLesson.java](StreamingLesson.java)

### support의 역할

이 단원에는 별도의 support가 필요하지 않다.

### 테스트

- [StreamingLessonTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l03streaming/StreamingLessonTest.java)

저장소 루트에서 실행:

```bash
./gradlew :Spring-AI-Tutor:test --tests 'com.example.shopping.tutor.lesson.l03streaming.*'
```

태그가 붙은 LIVE/MCP 테스트는 전용 scripts/test_live.sh, test_mcp.sh, test_live_mcp.sh로 실행한다.

## 웹 실습

| caseId | 학습 목표 | 예시 질문 |
|---|---|---|
| L03-01 | 스트리밍 응답 | 텀블러 고르는 기준 알려줘 |
| L03-02 | 생성 취소 | 선물 선택 기준을 설명해줘 |
| L03-03 | 부분 응답과 실패 | 텀블러 선택 가이드 [partial-error] |

## 관찰과 변경 실험

STUB partial-error 실험에서 부분 응답 후 failed가 발생하고 completed는 없는지 확인한다.

학습 메소드는 필요한 Spring AI 객체와 일반 입력만 받는다. caseId 연결, 요청 검증, 세션 격리, 실패 턴 보호는 platform의 책임이다.
