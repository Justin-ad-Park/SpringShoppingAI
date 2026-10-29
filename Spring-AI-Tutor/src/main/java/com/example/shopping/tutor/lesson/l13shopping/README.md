# 13. 검색 조건·상품 사실·배송 계산·RAG 통합

선수 단원: 02, 04, 06, 09.

## 읽는 순서

ShoppingAssistantLesson의 조건 추출 → SearchCriteriaReducer 병합 → 원본 상품 검색 → Java 견적 → 모델 설명을 따라간다.

1. 아래 핵심 Java 파일의 해당 실습 메소드를 읽는다.
2. 호출하는 도구·설정 파일을 따라간다.
3. 데이터가 궁금할 때만 support를 연다.
4. 아래 단원 테스트를 실행하고 질문·옵션을 바꿔 본다.

### 핵심 파일

- [ShoppingAssistantLesson.java](ShoppingAssistantLesson.java)

### support의 역할

이 단원에는 별도의 support가 필요하지 않다.

### 테스트

- [ShoppingAssistantLessonTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l13shopping/ShoppingAssistantLessonTest.java)

저장소 루트에서 실행:

```bash
./gradlew :Spring-AI-Tutor:test --tests 'com.example.shopping.tutor.lesson.l13shopping.*'
```

태그가 붙은 LIVE/MCP 테스트는 전용 scripts/test_live.sh, test_mcp.sh, test_live_mcp.sh로 실행한다.

## 웹 실습

| caseId | 학습 목표 | 예시 질문 |
|---|---|---|
| L13-01 | 후속 조건 상품 추천 | 3만원 이하 선물 찾아줘 |
| L13-02 | 무료배송 추천 통합 | 무료배송 채울 상품 추천해줘 |
| L13-03 | 상품 도구와 정책 RAG | 텀블러 추천하고 반품 조건도 알려줘 |
| L13-04 | 실패 후 복구·초기화 | 3만원 이하 음료 찾아줘 |

## 관찰과 변경 실험

후속 질문에서 조건을 유지하고 예산 해제로 삭제한다. timeout 후 이전 조건이 보존되는지 회귀 테스트에서 확인한다.

학습 메소드는 필요한 Spring AI 객체와 일반 입력만 받는다. caseId 연결, 요청 검증, 세션 격리, 실패 턴 보호는 platform의 책임이다.

추가 준비 코드: [FreeShippingRecommendationPolicy](support/FreeShippingRecommendationPolicy.java)는 기본 예산과 배송그룹·기존 상품 제외 규칙을 계산한다. 핵심 모델 호출과 통합 흐름은 ShoppingAssistantLesson에 남겨 두었다.
