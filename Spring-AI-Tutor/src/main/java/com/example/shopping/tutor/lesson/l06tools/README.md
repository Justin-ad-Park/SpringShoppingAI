# 06. AI가 실제 상품을 찾게 해 보세요

오늘은 [ProductSearchTools.java](ProductSearchTools.java)의 **searchProducts()**부터 읽습니다.

## 1. AI가 상품을 어떻게 알까요?

AI는 우리 상품 목록을 모릅니다. 대신 상품을 찾는 Java 메소드를 준비합니다.
category가 “음료”, maxPrice가 10000이면 만원 이하 음료를 찾습니다.
@Tool에는 이 메소드를 언제 쓰는지 설명합니다.
AI가 Java 메소드를 사용하도록 연결하는 기능을 Tool Calling이라고 합니다.

## 2. 먼저 Java 코드만 실행하기

[ProductSearchToolsTest](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l06tools/ProductSearchToolsTest.java)의
가격이_만원_이하인_음료를_찾는다를 실행하세요.
P004 허브티는 9,900원이므로 나옵니다. P005 커피는 12,500원이므로 나오지 않습니다.
API 키나 웹 서버 없이 확인할 수 있습니다.
저장소 루트에서 `./Spring-AI-Tutor/scripts/test_beginner.sh`를 실행하면 첫 호출·상품 검색·대화 기억 테스트를 함께 확인할 수 있습니다.

## 3. 한 가지 바꾸기

테스트의 10000을 15000으로 바꾸세요. 어떤 상품이 더 나올지 먼저 예상하세요.
기존 P004만 기대하는 확인문은 실패해야 합니다. P005도 기대하도록 바꾸고 다시 실행하세요.
가격을_만오천원으로_올리면_커피도_나온다에 완성된 비교 예제가 있습니다.

## 4. AI와 연결해 확인하기

웹 **L06-01**에서 “15000원 이하 음료를 추천해줘”를 보내세요.
오른쪽 “AI가 사용한 Java 메소드”에서 다음을 확인합니다.
- 메소드 이름이 searchProducts인가요?
- 전달한 category와 maxPrice는 무엇인가요?
- Java가 조회한 상품 ID와 가격은 무엇인가요?

STUB은 검색 인자를 음료·15000원으로 고정합니다. 웹 질문에 따라 검색 인자가 달라지는 실습은 LIVE에서 합니다.
상품 검색 자체의 조건은 Java 테스트로 먼저 확인하세요.

## 필요할 때만 더 읽기

- [ProductToolCallingLesson.callTool()](ProductToolCallingLesson.java): AI 요청과 Java 메소드 연결.
- [support/DemoProductCatalog.java](support/DemoProductCatalog.java): 준비된 상품과 검색 조건.
- [support/Product.java](support/Product.java): 상품의 ID·이름·가격을 담는 형태.
- L06-02와 [CartSummaryTools](CartSummaryTools.java): 배송비 계산.
- L06-03~05: 고객 구분, 반복 실행 제한, 장바구니 변경 확인.
- [ProductToolCallingLessonTest](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l06tools/ProductToolCallingLessonTest.java): 위 추가 동작의 확인.

다음은 [04 앞 대화 기억하기](../l04memory/README.md)입니다.
