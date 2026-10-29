# 09. 검색·컨텍스트·답변·출처·질문 재작성

선수 단원: 04, 07, 08.

## 읽는 순서

PolicyRagLesson → PolicyVectorIndex → PolicyDocumentFixtures 순으로 읽는다. 모델 호출 전에 몰·현재 버전을 필터한다.

1. 아래 핵심 Java 파일의 해당 실습 메소드를 읽는다.
2. 호출하는 도구·설정 파일을 따라간다.
3. 데이터가 궁금할 때만 support를 연다.
4. 아래 단원 테스트를 실행하고 질문·옵션을 바꿔 본다.

### 핵심 파일

- [PolicyRagLesson.java](PolicyRagLesson.java)
- [PolicyVectorIndex.java](PolicyVectorIndex.java)

### support의 역할

- [DocumentDisplay.java](support/DocumentDisplay.java): 검색 문서와 score를 관찰 패널에 표시할 자료로 변환.
- [PolicyDocumentFixtures.java](support/PolicyDocumentFixtures.java): 현재/과거/다른 몰의 가상 정책 원문.

### 테스트

- [PolicyRagLessonTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l09rag/PolicyRagLessonTest.java)

저장소 루트에서 실행:

```bash
./gradlew :Spring-AI-Tutor:test --tests 'com.example.shopping.tutor.lesson.l09rag.*'
```

태그가 붙은 LIVE/MCP 테스트는 전용 scripts/test_live.sh, test_mcp.sh, test_live_mcp.sh로 실행한다.

## 웹 실습

| caseId | 학습 목표 | 예시 질문 |
|---|---|---|
| L09-01 | 수동 RAG 파이프라인 | 미개봉 상품 반품 조건은? |
| L09-02 | QuestionAnswerAdvisor | 미개봉 상품 반품 조건은? |
| L09-03 | 근거 없는 질문 | 해외 배송 관세는? |
| L09-04 | 몰·버전 필터와 비신뢰 문서 | 현재 몰의 반품 조건은? |
| L09-05 | 후속 질문 재작성 | 미개봉 상품 반품 조건은? |

## 관찰과 변경 실험

관세 질문이나 STUB empty-retrieval에서 모델을 호출하지 않는지 확인한다. 반품 질문 다음 “그럼 뜯은 건?”으로 재작성을 관찰한다.

학습 메소드는 필요한 Spring AI 객체와 일반 입력만 받는다. caseId 연결, 요청 검증, 세션 격리, 실패 턴 보호는 platform의 책임이다.
