# 01. ChatModel·ChatClient·역할·프롬프트·옵션

선수 단원: 00.

## 읽는 순서

System/User 메시지, 렌더링된 프롬프트, usage를 관찰한다. 템플릿에 넣은 사용자 문자열은 다시 템플릿으로 해석하지 않는다.

1. 아래 핵심 Java 파일의 해당 실습 메소드를 읽는다.
2. 호출하는 도구·설정 파일을 따라간다.
3. 데이터가 궁금할 때만 support를 연다.
4. 아래 단원 테스트를 실행하고 질문·옵션을 바꿔 본다.

### 핵심 파일

- [BasicChatLesson.java](BasicChatLesson.java)
- [PromptTemplateLesson.java](PromptTemplateLesson.java)

### support의 역할

이 단원에는 별도의 support가 필요하지 않다.

### 테스트

- [BasicChatLessonTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l01chat/BasicChatLessonTest.java)

저장소 루트에서 실행:

```bash
./gradlew :Spring-AI-Tutor:test --tests 'com.example.shopping.tutor.lesson.l01chat.*'
```

태그가 붙은 LIVE/MCP 테스트는 전용 scripts/test_live.sh, test_mcp.sh, test_live_mcp.sh로 실행한다.

## 웹 실습

| caseId | 학습 목표 | 예시 질문 |
|---|---|---|
| L01-01 | ChatModel과 ChatClient 비교 | 선물 고를 때 고려할 점 3개 |
| L01-02 | System과 User 역할 | 지금 텀블러 재고가 몇 개야? |
| L01-03 | 템플릿 변수 | 예산 3만원, 동료 선물 추천 기준 |
| L01-04 | 옵션과 토큰 사용량 | 선물 추천 기준 알려줘 |

## 관찰과 변경 실험

temperature를 0과 1로 비교하고, 없는 재고를 물어 모델의 지식 한계를 확인한다.

학습 메소드는 필요한 Spring AI 객체와 일반 입력만 받는다. caseId 연결, 요청 검증, 세션 격리, 실패 턴 보호는 platform의 책임이다.
