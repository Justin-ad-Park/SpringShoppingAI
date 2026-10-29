# 06. 모델의 도구 선택과 Java 실행

선수 단원: 01, 02.

## 읽는 순서

ProductToolCallingLesson → ProductSearchTools → CartSummaryTools 순으로 읽는다. callback은 웹 어댑터가 등록하고 관찰 상한을 덧씌운다.

1. 아래 핵심 Java 파일의 해당 실습 메소드를 읽는다.
2. 호출하는 도구·설정 파일을 따라간다.
3. 데이터가 궁금할 때만 support를 연다.
4. 아래 단원 테스트를 실행하고 질문·옵션을 바꿔 본다.

### 핵심 파일

- [CartSummaryTools.java](CartSummaryTools.java)
- [ProductSearchTools.java](ProductSearchTools.java)
- [ProductToolCallingLesson.java](ProductToolCallingLesson.java)

### support의 역할

- [DemoCartRepository.java](support/DemoCartRepository.java): standard/already-free/empty/no-match 합계 fixture.
- [DemoProductCatalog.java](support/DemoProductCatalog.java): 가상 상품 원본·재고·검색. 실제 판매 DB를 사용하지 않는다.
- [Product.java](support/Product.java): 상품 ID·가격·배송그룹·재고를 담는 record.
- [SandboxCart.java](support/SandboxCart.java): 소유자·만료·중복 확인을 검사하는 데모 변경 저장소.
- [ShippingPolicy.java](support/ShippingPolicy.java): 원 단위 배송비와 무료배송 경계 계산.
- [ShippingQuoteCalculator.java](support/ShippingQuoteCalculator.java): 후보 추가 가정의 견적. 실제 장바구니는 바꾸지 않는다.

### 테스트

- [ProductToolCallingLessonTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l06tools/ProductToolCallingLessonTest.java)

저장소 루트에서 실행:

```bash
./gradlew :Spring-AI-Tutor:test --tests 'com.example.shopping.tutor.lesson.l06tools.*'
```

태그가 붙은 LIVE/MCP 테스트는 전용 scripts/test_live.sh, test_mcp.sh, test_live_mcp.sh로 실행한다.

## 웹 실습

| caseId | 학습 목표 | 예시 질문 |
|---|---|---|
| L06-01 | 상품 검색 Tool Calling | 1만5천원 이하 음료 찾아줘 |
| L06-02 | 무료배송 금액 계산 | 무료배송까지 얼마나 부족해? |
| L06-03 | 신뢰할 ToolContext | 내 장바구니 확인해줘 |
| L06-04 | 도구 실행 상한 | 도구를 9번 실행하는 실험 |
| L06-05 | 샌드박스 변경 확인 | P004 하나 담아줘 |

## 관찰과 변경 실험

존재하지 않는 상품, 품절, 다른 배송그룹을 견적에 넣어 본다. 샌드박스는 확인 전 변경되지 않는다.

학습 메소드는 필요한 Spring AI 객체와 일반 입력만 받는다. caseId 연결, 요청 검증, 세션 격리, 실패 턴 보호는 platform의 책임이다.
