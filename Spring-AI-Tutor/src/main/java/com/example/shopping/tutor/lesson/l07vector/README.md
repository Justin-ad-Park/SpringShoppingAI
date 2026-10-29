# 07. 임베딩·cosine·벡터 검색·필터

선수 단원: 06.

## 읽는 순서

EmbeddingLesson → VectorStoreLesson → EmbeddingModelConfiguration 순서로 읽는다. SimpleVectorStore 생성·add·search·delete를 확인한다.

1. 아래 핵심 Java 파일의 해당 실습 메소드를 읽는다.
2. 호출하는 도구·설정 파일을 따라간다.
3. 데이터가 궁금할 때만 support를 연다.
4. 아래 단원 테스트를 실행하고 질문·옵션을 바꿔 본다.

### 핵심 파일

- [EmbeddingLesson.java](EmbeddingLesson.java)
- [EmbeddingModelConfiguration.java](EmbeddingModelConfiguration.java)
- [VectorStoreLesson.java](VectorStoreLesson.java)

### support의 역할

- [FixedEmbeddingModel.java](support/FixedEmbeddingModel.java): 교육용 단어군을 고정 차원으로 변환. 실제 의미 이해 모델이 아니다.

### 테스트

- [VectorStoreLessonTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l07vector/VectorStoreLessonTest.java)

저장소 루트에서 실행:

```bash
./gradlew :Spring-AI-Tutor:test --tests 'com.example.shopping.tutor.lesson.l07vector.*'
```

태그가 붙은 LIVE/MCP 테스트는 전용 scripts/test_live.sh, test_mcp.sh, test_live_mcp.sh로 실행한다.

## 웹 실습

| caseId | 학습 목표 | 예시 질문 |
|---|---|---|
| L07-01 | 임베딩과 cosine 유사도 | 보온용 컵 |
| L07-02 | 벡터 저장·검색·삭제 | 따뜻한 차 마실 용품 |
| L07-03 | 메타데이터 필터 | 생활 카테고리 중 재고 있는 것 |
| L07-04 | pgvector 저장소 교체 | 생활용품을 pgvector에서 검색해줘 |

## 관찰과 변경 실험

차원이 다른 벡터를 비교하면 실패한다. pgvector는 전용 DB가 설정된 경우에만 실행한다.

학습 메소드는 필요한 Spring AI 객체와 일반 입력만 받는다. caseId 연결, 요청 검증, 세션 격리, 실패 턴 보호는 platform의 책임이다.
