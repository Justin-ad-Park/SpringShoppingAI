# 04-2. 응답을 record로 받기

**실행 코드·설정·테스트:** [labs/04-structured-output](../../../labs/04-structured-output/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

30~45분을 기준으로 한 번에 한 가지 흐름만 읽습니다. 읽기와 실습을 합친 시간이며, 단원 전체 예상 시간에 포함됩니다.

**이번 목표:** 문자열 응답과 객체 응답의 반환 타입과 사용 방법을 비교합니다.

**코드 읽기 안내:** 먼저 결과 객체의 필드를 읽고, 문자열로 받을 때와 객체로 받을 때의 호출을 비교합니다.

## 코드 읽는 순서

모듈: `labs/04-structured-output`, 패키지: `com.example.ailearning.l04structured`.
아래 파일은 교육 폴더 기준이며 실제 파일을 그대로 읽을 수 있습니다.

1. [SearchCriteria.java](../../../labs/04-structured-output/src/main/java/com/example/ailearning/l04structured/SearchCriteria.java): 결과 필드. 검증 메서드는 다음 소수업에서 읽습니다.
2. [CriteriaService.java](../../../labs/04-structured-output/src/main/java/com/example/ailearning/l04structured/CriteriaService.java): 모델 요청과 변환
3. [CriteriaController.java](../../../labs/04-structured-output/src/main/java/com/example/ailearning/l04structured/CriteriaController.java): HTTP 입력 연결
4. [application.yml](../../../labs/04-structured-output/src/main/resources/application.yml), [build.gradle](../../../labs/04-structured-output/build.gradle): 루트 `.env`의 키·모델을 사용하는 방식은 첫 호출 실습과 같습니다. 포트는 8094, 채팅 타임아웃은 15초입니다(첫 호출은 30초).

### 결과 객체의 필드

다음은 `SearchCriteria`의 **필드 구성을 보여 주는 구조 예시**입니다. 실제 파일에는 다음 소수업에서 읽을 `validate()`도 있습니다.

```java
public record SearchCriteria(String category, Integer maxPrice) {}
```

| 필드 | 이 예제에서 담는 값 |
|---|---|
| `category` | 사용자가 말한 상품 카테고리 |
| `maxPrice` | 사용자가 말한 상품 한 개의 가격 상한(원) |

### 문자열로 받기와 객체로 받기

아래 두 코드는 `CriteriaService.java`에서 발췌했습니다. `client`는 생성자에서 Builder로 준비한 ChatClient이고,
`INSTRUCTION`은 카테고리와 가격 상한을 추출하라는 system 지시입니다. 전체 선언은 위 소스 링크에서 확인합니다.

문자열로 받는 `text()`:

```java
public String text(String question) {
    return client.prompt().system(INSTRUCTION).user(question).call().content();
}
```

객체로 받는 `extract()` 안의 변환 부분:

```java
criteria = client.prompt().system(INSTRUCTION).user(question)
        .call().entity(SearchCriteria.class);
```

`.content()`는 문자열을 반환하고, `.entity(SearchCriteria.class)`는 응답 형식 지시를 추가하고 결과를 지정한 타입으로 변환합니다.
두 경로는 모델에 전달되는 형식 지시가 다릅니다.

객체를 받은 뒤 필드를 읽는 **사용 예시**입니다. `criteria`는 변환에 성공해 받은 객체라고 가정합니다.

```java
String category = criteria.category();
Integer maxPrice = criteria.maxPrice();
```

문장을 직접 잘라 값을 찾는 대신 접근자로 각 값을 읽습니다. 변환 실패와 허용되지 않는 값 처리는 [다음 소수업](03-validate.md)에서 실제 코드와 함께 확인합니다.

## 여기서 멈춰 확인하기

문자열로 받은 응답과 객체로 받은 응답은 각각 어떤 타입이며, 객체의 가격 필드는 어떻게 읽나요?

[이전](01-format.md) · [다음](03-validate.md) · [단원 목차](../04-structured-output.md) · [학습 방법](../../learning-guide.md)
