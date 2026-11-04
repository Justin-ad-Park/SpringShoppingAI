# 04-1. 구조화 응답을 이해하기 위한 기초 용어

**실행 코드·설정·테스트:** [labs/04-structured-output](../../../labs/04-structured-output/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** JSON, Java record, Integer의 의미를 알고, 구조화 응답이 무엇인지 설명할 수 있습니다.

## 해결할 문제

“음료 중 1만5천원 이하 선물을 찾아줘”를 Java 코드에서 처리하려면 카테고리와 가격을 각각 읽어야 합니다.
문장을 `split()`으로 나누면 표현이 바뀔 때 깨집니다. 모델에게 필요한 필드를 요청하고 객체로 변환해 보겠습니다.

## 새 용어: record, JSON, 구조화 응답

- Java `record`는 값을 묶는 자료형입니다. `record SearchCriteria(String category, Integer maxPrice)`는 생성자와 `category()`, `maxPrice()` 접근자를 제공합니다.
- JSON은 이름과 값으로 데이터를 표현하는 텍스트입니다. `{"category":"음료","maxPrice":15000}`처럼 씁니다.
- `Integer`는 정수 또는 `null`을 담을 수 있는 Java 자료형입니다. `int`는 `null`을 담을 수 없습니다.
- 구조화 응답은 모델의 결과를 정해진 자료형으로 사용하는 방식입니다. 형식이 맞아도 값이 맞는지는 별도 검증합니다.

실제 호출 코드는 [04-2](02-convert.md)에서 읽고, 코드와 객체의 관계는 [04-3의 구조와 변환 순서](03-validate.md#구조와-변환-순서)에서 그림으로 정리합니다.

## 여기서 멈춰 확인하기

1. JSON과 Java record는 각각 무엇을 표현하나요?
2. `Integer`와 `int`는 `null`을 담을 수 있는지에서 어떤 차이가 있나요?
3. 구조화 응답은 무엇인가요?

[이전](../04-structured-output.md) · [다음](02-convert.md) · [단원 목차](../04-structured-output.md) · [학습 방법](../../learning-guide.md)
